package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }

    LaunchedEffect(true) {
        db.collection("users").document(uid)
            .collection("tasks")
            .addSnapshotListener { snap, _ ->
                tasks = snap?.documents?.mapNotNull { doc ->
                    TaskItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        subject = doc.getString("subject") ?: "",
                        category = doc.getString("category") ?: "",
                        difficulty = doc.getString("difficulty") ?: "",
                        due = doc.getString("due") ?: "",
                        remindBeforeMinutes = doc.getLong("remindBeforeMinutes") ?: 0L,
                        completed = doc.getBoolean("completed") ?: false
                    )
                } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
                Text("Task", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            Text("Tasks", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    "Scan notes",
                    modifier = Modifier.clickable { nav.navigate("scan") },
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Add",
                    modifier = Modifier.clickable { nav.navigate("task_add") },
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Smart reminder",
                    modifier = Modifier.clickable { nav.navigate("smart") },
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(18.dp))

            LazyColumn {
                items(tasks) { task ->
                    TaskCard(
                        task = task,
                        onDone = {
                            val pts = when (task.difficulty.lowercase()) {
                                "easy" -> 10
                                "medium" -> 20
                                "hard" -> 30
                                else -> 0
                            }

                            db.collection("users").document(uid)
                                .collection("tasks").document(task.id)
                                .update("completed", true)

                            db.collection("users").document(uid)
                                .update("points", FieldValue.increment(pts.toLong()))
                        },
                        onDelete = {
                            db.collection("users").document(uid)
                                .collection("tasks").document(task.id)
                                .delete()
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Subject: ${task.subject}")
            Text("Category: ${task.category}")
            Text("Difficulty: ${task.difficulty}")
            Text("Due: ${task.due}")
            Text("Reminder: ${task.remindBeforeMinutes} min")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDone) {
                    Icon(Icons.Filled.Done, null, tint = Color.Blue)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null, tint = Color.Red)
                }
            }
        }
    }
}
