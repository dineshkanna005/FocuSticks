package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class LeaderUser(
    val uid: String = "",
    val name: String = "",
    val points: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var list by remember { mutableStateOf(listOf<LeaderUser>()) }

    LaunchedEffect(true) {
        db.collection("users")
            .addSnapshotListener { snap, _ ->
                val users = snap?.documents?.mapNotNull { doc ->
                    LeaderUser(
                        uid = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        points = doc.getLong("points")?.toInt() ?: 0
                    )
                } ?: emptyList()

                list = users.sortedByDescending { it.points }.take(10)
            }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                }
                Text("Leaderboard", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Name", style = MaterialTheme.typography.titleMedium)
                Text("Points", style = MaterialTheme.typography.titleMedium)
            }

            LazyColumn {
                itemsIndexed(list) { index, user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${index + 1}. ${user.name}")
                        Text("${user.points}")
                    }
                }
            }
        }
    }
}
