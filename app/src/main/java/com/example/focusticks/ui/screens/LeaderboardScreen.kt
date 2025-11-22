package com.example.focusticks.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class LeaderUser(val name: String, val points: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(nav: NavHostController) {

    var list by remember { mutableStateOf(listOf<LeaderUser>()) }
    val db = Firebase.firestore

    DisposableEffect(Unit) {
        val listener = db.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    list = snap.documents.map {
                        LeaderUser(
                            name = it.getString("name") ?: "User",
                            points = it.getLong("points") ?: 0L
                        )
                    }
                }
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Leaderboard", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Name")
                Text("Points")
            }

            Spacer(Modifier.height(8.dp))

            list.forEach { u ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(u.name.ifBlank { "User" })
                    Text(u.points.toString())
                }
            }
        }
    }
}
