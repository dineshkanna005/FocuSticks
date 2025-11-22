package com.example.focusticks.ui.screens.task

import android.app.NotificationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.focusticks.scheduleReminder
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: ""
    val db = Firebase.firestore
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var remind by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
                Text("Add Task", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = subject, onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = difficulty, onValueChange = { difficulty = it },
                label = { Text("Difficulty") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = due, onValueChange = { due = it },
                label = { Text("Due (MM/dd/yyyy HH:mm)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = remind, onValueChange = { remind = it },
                label = { Text("Remind Before (minutes)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (uid.isNotEmpty() && title.isNotBlank()) {

                        db.collection("tasks")
                            .add(
                                mapOf(
                                    "title" to title.trim(),
                                    "subject" to subject.trim().ifBlank { "General" },
                                    "difficulty" to difficulty.trim().ifBlank { "Medium" },
                                    "category" to category.trim().ifBlank { "Assignment" },
                                    "due" to due.trim(),
                                    "remindBeforeMinutes" to (remind.toLongOrNull() ?: 10),
                                    "completed" to false,
                                    "createdAt" to Timestamp.now(),
                                    "uid" to uid
                                )
                            )
                            .addOnSuccessListener { doc ->

                                val notification = NotificationCompat.Builder(context, "task_channel")
                                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                                    .setContentTitle("Task Created")
                                    .setContentText("New Task: ${title.trim()}")
                                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
                                    .build()

                                NotificationManagerCompat.from(context)
                                    .notify(doc.id.hashCode(), notification)

                                scheduleReminder(
                                    context,
                                    doc.id,
                                    title.trim(),
                                    due.trim(),
                                    remind.toLongOrNull() ?: 10
                                )

                                nav.popBackStack()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
