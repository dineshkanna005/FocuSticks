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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: ""
    val db = Firebase.firestore

    var totalPoints by remember { mutableStateOf(0L) }
    var days by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    totalPoints = snap.getLong("points") ?: 0
                    days = (snap.getLong("streakDays") ?: 0L).toInt()
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
                Text("Streaks", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Points: $totalPoints", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text("Days Active: $days", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = { nav.navigate("task") },
                modifier = Modifier.fillMaxWidth().height(55.dp)
            ) { Text("View Tasks") }
        }
    }
}
