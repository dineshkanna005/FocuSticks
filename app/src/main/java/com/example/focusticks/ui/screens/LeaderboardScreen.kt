package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.focusticks.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val currentUid = Firebase.auth.currentUser?.uid
    var leaderboard by remember { mutableStateOf(listOf<User>()) }
    var friendIds by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (currentUid != null) {
            db.collection("users")
                .document(currentUid)
                .collection("friends")
                .addSnapshotListener { snap, _ ->
                    friendIds = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                }
        }

        db.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val allUsers = snap?.documents?.map { doc ->
                    User(
                        uid = doc.id,
                        name = doc.getString("name") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        phoneNo = doc.getString("phoneNo") ?: "",
                        email = doc.getString("email") ?: "",
                        dob = doc.getString("dob") ?: "",
                        points = doc.getLong("points") ?: 0L,
                        lastTaskCompleted = doc.getLong("lastTaskCompleted") ?: 0L
                    )
                } ?: emptyList()

                leaderboard = allUsers.filter { it.uid == currentUid || it.uid in friendIds }
                isLoading = false
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
                    Text("Leaderboard", style = MaterialTheme.typography.headlineSmall)
                }

                IconButton(onClick = { openDrawer() }) {
                    Icon(Icons.Filled.Menu, null)
                }
            }
        }
    ) { pad ->

        if (isLoading) {
            Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            itemsIndexed(leaderboard) { index, user ->

                val rank = index + 1
                val isCurrentUser = currentUid == user.uid

                val medalColor = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                val container =
                    if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = container),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(medalColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(rank.toString(), fontSize = 18.sp)
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                user.name.ifBlank { "Anonymous" },
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isCurrentUser) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "(You)",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${user.points}", fontSize = 18.sp)
                            if (rank <= 3) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.Star, null, tint = medalColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
