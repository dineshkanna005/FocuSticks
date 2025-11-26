package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase

@Composable
fun LeaderboardScreen(nav: NavHostController) {

    val db = Firebase.firestore
    val currentUid = Firebase.auth.currentUser?.uid
    var leaderboard by remember { mutableStateOf(listOf<User>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                leaderboard = snap?.documents?.mapNotNull { doc ->
                    doc.toObject<User>()?.copy(uid = doc.id)
                } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Leaderboard 🏆", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { pad ->

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(leaderboard) { index, user ->
                val rank = index + 1
                val isCurrentUser = user.uid == currentUid
                val cardColor = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else cardColor
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$rank.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(30.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(user.name.ifBlank { "Anonymous" }, style = MaterialTheme.typography.titleMedium)
                            if (isCurrentUser) {
                                Spacer(Modifier.width(8.dp))
                                Text("(You)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${user.points} points", style = MaterialTheme.typography.bodyLarge)
                            if (rank <= 3) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Filled.Star, contentDescription = "Top Rank", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}