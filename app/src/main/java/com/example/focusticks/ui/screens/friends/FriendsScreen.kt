package com.example.focusticks.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class FriendUser(
    val uid: String,
    val name: String,
    val points: Long,
    val streak: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val currentUid = Firebase.auth.currentUser?.uid ?: return
    val currentName = Firebase.auth.currentUser?.displayName ?: "Someone"

    var allUsers by remember { mutableStateOf(listOf<FriendUser>()) }
    var following by remember { mutableStateOf(setOf<String>()) }

    val highlightUid = nav.currentBackStackEntry
        ?.arguments
        ?.getString("highlightUid")

    var flashUid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(highlightUid) {
        if (highlightUid != null) {
            flashUid = highlightUid
            kotlinx.coroutines.delay(1000)
            flashUid = null
        }
    }

    LaunchedEffect(true) {
        db.collection("users").get().addOnSuccessListener { snap ->
            val result = snap.documents.mapNotNull {
                val uid = it.id
                if (uid == currentUid) return@mapNotNull null
                FriendUser(
                    uid = uid,
                    name = it.getString("name") ?: "User",
                    points = it.getLong("points") ?: 0,
                    streak = it.getLong("streak") ?: 0
                )
            }
            allUsers = result
        }

        db.collection("users").document(currentUid)
            .collection("friends")
            .get()
            .addOnSuccessListener { snap ->
                following = snap.documents.map { it.id }.toSet()
            }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text("Friends", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Menu",
                    modifier = Modifier.clickable { openDrawer() },
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .padding(20.dp)
                .fillMaxSize()
        ) {

            LazyColumn(Modifier.fillMaxSize()) {
                items(allUsers) { user ->

                    val isFollowing = following.contains(user.uid)

                    val bg = if (flashUid == user.uid)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.surface

                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(bg),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clickable {
                                            nav.navigate("friendTasks/${user.uid}")
                                        }
                                ) {
                                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Points: ${user.points}")
                                    Text("Streak: ${user.streak}")
                                }

                                Button(
                                    onClick = {
                                        if (isFollowing) {
                                            db.collection("users").document(currentUid)
                                                .collection("friends")
                                                .document(user.uid)
                                                .delete()
                                            following = following - user.uid
                                        } else {
                                            db.collection("users").document(currentUid)
                                                .collection("friends")
                                                .document(user.uid)
                                                .set(mapOf("followed" to true))

                                            db.collection("users").document(user.uid)
                                                .collection("alerts")
                                                .add(
                                                    mapOf(
                                                        "message" to "$currentName followed you!",
                                                        "seen" to false,
                                                        "timestamp" to System.currentTimeMillis()
                                                    )
                                                )

                                            following = following + user.uid
                                        }
                                    }
                                ) {
                                    Text(if (isFollowing) "Unfollow" else "Follow")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
