package com.example.focusticks.ui.screens.friends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class FriendAchievement(
    val id: String,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendTasksScreen(nav: NavHostController, friendUid: String?) {

    val db = Firebase.firestore
    var friendName by remember { mutableStateOf("Friend") }
    var streak by remember { mutableStateOf(0L) }
    var points by remember { mutableStateOf(0L) }
    var achievements by remember { mutableStateOf(listOf<FriendAchievement>()) }

    LaunchedEffect(friendUid) {
        if (friendUid == null) return@LaunchedEffect

        db.collection("users").document(friendUid)
            .get()
            .addOnSuccessListener { doc ->
                friendName = doc.getString("name") ?: "Friend"
                streak = doc.getLong("streak") ?: 0
                points = doc.getLong("points") ?: 0
            }

        db.collection("users").document(friendUid)
            .collection("achievements")
            .get()
            .addOnSuccessListener { snap ->
                val result = snap.documents.map {
                    FriendAchievement(
                        id = it.id,
                        title = it.getString("title") ?: ""
                    )
                }
                achievements = result
            }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text("$friendName’s Stats", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .padding(20.dp)
                .fillMaxSize()
        ) {

            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Points: $points", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Streak: $streak days", style = MaterialTheme.typography.titleMedium)
                }
            }

            Text(
                "Achievements",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (achievements.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text("No achievements yet")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(achievements) { a ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(a.title, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
