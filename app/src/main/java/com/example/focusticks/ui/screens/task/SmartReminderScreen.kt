package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.NotificationHelper
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReminderScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: ""
    val db = Firebase.firestore

    var hardestTask by remember { mutableStateOf<TaskItem?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("tasks")
            .whereEqualTo("uid", uid)
            .whereEqualTo("completed", false)
            .get()
            .addOnSuccessListener { snap ->

                val df12 = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US)
                val df24 = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
                val now = System.currentTimeMillis()
                val window = now + 24L * 60L * 60L * 1000L

                val items = snap.documents.mapNotNull { doc ->
                    val dueString = doc.getString("due") ?: return@mapNotNull null
                    val dueDate =
                        try { df12.parse(dueString) }
                        catch (_: Exception) {
                            try { df24.parse(dueString) }
                            catch (_: Exception) { null }
                        } ?: return@mapNotNull null

                    val diffText = doc.getString("difficulty")?.lowercase()?.trim() ?: ""
                    val diffScore =
                        when {
                            diffText.contains("hard") -> 3
                            diffText.contains("medium") -> 2
                            diffText.contains("easy") -> 1
                            else -> 0
                        }

                    Triple(
                        TaskItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            subject = doc.getString("subject") ?: "",
                            category = doc.getString("category") ?: "",
                            difficulty = doc.getString("difficulty") ?: "",
                            due = dueString,
                            remindBefore = doc.getLong("remindBeforeMinutes") ?: 0L,
                            completed = false,
                            completedAt = ""
                        ),
                        dueDate.time,
                        diffScore
                    )
                }

                val urgent = items.filter { it.second in now..window }
                val best = urgent.maxByOrNull { it.third }

                hardestTask = best?.first
                loading = false
            }
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
                Text("Smart Reminder", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (loading) {
                Text("Analyzing your tasks…")
            } else if (hardestTask == null) {
                Text("No urgent or hard tasks in the next 24 hours.")
            } else {
                Text(hardestTask!!.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Difficulty: ${hardestTask!!.difficulty}")
                Text("Due: ${hardestTask!!.due}")
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        NotificationHelper.showReminderNotification(
                            context = nav.context,
                            title = "Smart Reminder: ${hardestTask!!.title} is due soon!",
                            taskId = hardestTask!!.id,
                            type = "reminder"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Smart Reminder")
                }
            }
        }
    }
}
