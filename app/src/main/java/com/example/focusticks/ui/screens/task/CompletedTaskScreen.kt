package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun CompletedTaskScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }
    var flashId by remember { mutableStateOf("") }

    LaunchedEffect(flashId) {
        if (flashId.isNotEmpty()) {
            kotlinx.coroutines.delay(800)
            flashId = ""
        }
    }

    LaunchedEffect(Unit) {
        db.collection("tasks")
            .whereEqualTo("uid", uid)
            .whereEqualTo("completed", true)
            .addSnapshotListener { snap, _ ->
                tasks = snap?.documents?.map { doc ->
                    TaskItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        subject = doc.getString("subject") ?: "",
                        category = doc.getString("category") ?: "",
                        difficulty = doc.getString("difficulty") ?: "",
                        due = doc.getString("due") ?: "",
                        remindBefore = doc.getLong("remindBeforeMinutes") ?: 0L,
                        completed = true,
                        completedAt = doc.getString("completedAt") ?: ""
                    )
                } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Completed Tasks", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { pad ->

        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            items(tasks) { t ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (flashId == t.id)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .padding(16.dp)
                    ) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Subject: ${t.subject}")
                        Text("Category: ${t.category}")
                        Text("Difficulty: ${t.difficulty}")
                        Text("Due: ${t.due}")
                        Text("Completed At: ${t.completedAt}")
                    }
                }
            }
        }
    }
}
