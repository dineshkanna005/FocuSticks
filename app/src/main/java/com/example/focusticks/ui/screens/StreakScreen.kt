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
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(nav: NavHostController) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var streak by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var points by remember { mutableStateOf(0L) }

    DisposableEffect(uid) {
        val reg1 = db.collection("tasks")
            .whereEqualTo("uid", uid)
            .whereEqualTo("completed", true)
            .addSnapshotListener { snap, _ ->
                val done = snap?.documents.orEmpty()
                total = done.size

                val days = done.mapNotNull {
                    val t = it.getTimestamp("completedAt") ?: return@mapNotNull null
                    t.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }.toSet()

                val today = LocalDate.now()
                var s = 0
                var cursor = today
                while (days.contains(cursor)) {
                    s++
                    cursor = cursor.minusDays(1)
                }
                streak = s
            }

        val reg2 = db.collection("users")
            .document(uid)
            .addSnapshotListener { snap, _ ->
                points = snap?.getLong("points") ?: 0L
            }

        onDispose {
            reg1.remove()
            reg2.remove()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text("Streak", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Current streak: $streak days", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Text("Completed tasks: $total", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Text("Total points: $points", style = MaterialTheme.typography.titleMedium)
        }
    }
}
