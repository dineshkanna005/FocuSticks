package com.example.focusticks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class AchievementItem(
    val id: String = "",
    val title: String = "",
    val emoji: String = "",
    val unlocked: Boolean = false
)

val allAchievements = listOf(
    AchievementItem("first5", "First 5 Tasks", "🔥", false),
    AchievementItem("hard3", "3 Hard Tasks", "💪", false),
    AchievementItem("streak7", "7 Day Streak", "⭐", false),
    AchievementItem("noOverdue", "No Overdue Week", "🚀", false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var unlocked by remember { mutableStateOf(listOf<String>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .document(uid)
            .collection("achievements")
            .addSnapshotListener { snap, _ ->
                unlocked = snap?.documents
                    ?.filter { it.getBoolean("unlocked") == true }
                    ?.map { it.id }
                    ?: emptyList()

                loading = false
            }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text(
                    "Achievements",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 10.dp)
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Menu,
                    null,
                    modifier = Modifier.clickable { openDrawer() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { pad ->

        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(allAchievements) { a ->

                val isUnlocked = a.id in unlocked

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isUnlocked) 1f else 0.4f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        if (isUnlocked)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(if (isUnlocked) 6.dp else 1.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(a.emoji, style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(a.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
