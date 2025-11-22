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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.focusticks.NotificationHelper
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

                val df = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
                val now = System.currentTimeMillis()
                val tomorrow = now + 24 * 60 * 60 * 1000

                val tasks = snap.documents.mapNotNull { doc ->
                    val dueString = doc.getString("due") ?: return@mapNotNull null
                    val dueDate = try { df.parse(dueString) } catch (_: Exception) { null }
                    val diff = when (doc.getString("difficulty")?.lowercase()) {
                        "hard" -> 3
                        "medium" -> 2
                        "easy" -> 1
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
                            remindBefore = doc.getLong("remindBeforeMinutes") ?: 0,
                            completed = false,
                            completedAt = ""
                        ),
                        dueDate,
                        diff
                    )
                }

                val urgent = tasks.filter { it.second != null && it.second!!.time in now..tomorrow }
                val hardest = urgent.maxByOrNull { it.third }

                hardestTask = hardest?.first
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
                            nav.context,
                            "Smart Reminder: ${hardestTask!!.title} is due soon!",
                            hardestTask!!.id
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
