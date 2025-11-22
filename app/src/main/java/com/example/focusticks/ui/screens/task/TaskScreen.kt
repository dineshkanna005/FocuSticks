package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.NotificationHelper
import com.example.focusticks.scheduleReminder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: ""
    val db = Firebase.firestore
    val ctx = LocalContext.current

    var list by remember { mutableStateOf(listOf<TaskItem>()) }
    var showEdit by remember { mutableStateOf<TaskItem?>(null) }
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
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    list = snap.documents.map {
                        TaskItem(
                            id = it.id,
                            title = it.getString("title") ?: "",
                            subject = it.getString("subject") ?: "",
                            category = it.getString("category") ?: "",
                            difficulty = it.getString("difficulty") ?: "",
                            due = it.getString("due") ?: "",
                            remindBefore = it.getLong("remindBeforeMinutes") ?: 0,
                            completed = it.getBoolean("completed") ?: false,
                            completedAt = it.getString("completedAt") ?: ""
                        )
                    }.filter { !it.completed }
                }
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
                Text("Task", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Scan Notes", Modifier.clickable { nav.navigate("scanNotes") }, color = Color(0xFF3F51B5))
                Text("Smart Reminder", Modifier.clickable { nav.navigate("smartReminder") }, color = Color(0xFF3F51B5))
                Text("Add", Modifier.clickable { nav.navigate("addTask") }, color = Color(0xFF3F51B5))
                Text("Completed", Modifier.clickable { nav.navigate("task_completed") }, color = Color(0xFF3F51B5))
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn {
                items(list) { t ->

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, shape = MaterialTheme.shapes.medium)
                            .background(
                                if (flashId == t.id) Color(0xFFB0BEC5) else Color(0xFFEFEFEF),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
                    ) {

                        Text(t.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Subject: ${t.subject}")
                        Text("Category: ${t.category}")
                        Text("Difficulty: ${t.difficulty}")
                        Text("Due: ${t.due}")

                        Spacer(Modifier.height(12.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Icon(
                                Icons.Filled.Edit,
                                null,
                                Modifier.size(24.dp).clickable { showEdit = t }
                            )

                            Icon(
                                Icons.Filled.Delete,
                                null,
                                Modifier.size(24.dp).clickable {
                                    db.collection("tasks").document(t.id).delete()
                                }
                            )

                            Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                Modifier.size(28.dp).clickable {
                                    if (!t.completed) {
                                        flashId = t.id
                                        val now = Timestamp.now()

                                        db.collection("tasks")
                                            .document(t.id)
                                            .set(
                                                mapOf(
                                                    "completed" to true,
                                                    "completedAt" to now.toDate().toString()
                                                ),
                                                SetOptions.merge()
                                            )

                                        updateUserPointsAndStreak(uid)

                                        NotificationHelper.showReminderNotification(
                                            ctx,
                                            "Task Completed: ${t.title}",
                                            t.id
                                        )
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }

            if (showEdit != null) {
                EditTaskDialog(
                    task = showEdit!!,
                    onDismiss = { showEdit = null },
                    onSave = { updated ->
                        db.collection("tasks").document(updated.id)
                            .update(
                                mapOf(
                                    "title" to updated.title,
                                    "subject" to updated.subject,
                                    "difficulty" to updated.difficulty,
                                    "category" to updated.category,
                                    "due" to updated.due,
                                    "remindBeforeMinutes" to updated.remindBefore
                                )
                            )

                        scheduleReminder(ctx, updated.id, updated.title, updated.due, updated.remindBefore)
                        showEdit = null
                    }
                )
            }
        }
    }
}

fun updateUserPointsAndStreak(uid: String) {
    val db = Firebase.firestore

    db.collection("tasks")
        .whereEqualTo("uid", uid)
        .whereEqualTo("completed", true)
        .get()
        .addOnSuccessListener { tasks ->

            val totalPoints = tasks.size() * 10

            val validDates = tasks.mapNotNull {
                val raw = it.getString("completedAt")
                if (raw != null && raw.length >= 10) raw.substring(0, 10) else null
            }.toSet()

            val streakDays = validDates.size
            val lastActive = validDates.maxOrNull() ?: ""

            db.collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "points" to totalPoints,
                        "streakDays" to streakDays,
                        "lastActiveDate" to lastActive
                    ),
                    SetOptions.merge()
                )
        }
}
