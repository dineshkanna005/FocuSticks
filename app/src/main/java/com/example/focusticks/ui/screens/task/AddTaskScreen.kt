package com.example.focusticks.ui.screens.task

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val uid = Firebase.auth.currentUser?.uid ?: return
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
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text("Add Task", style = MaterialTheme.typography.titleLarge)
                }
                Icon(
                    Icons.Filled.Menu,
                    "",
                    modifier = Modifier.clickable { openDrawer() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Task Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = difficulty,
                        onValueChange = { difficulty = it },
                        label = { Text("Difficulty") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Timing", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = due,
                        onValueChange = { due = it },
                        label = { Text("Due (MM/dd/yyyy HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = remind,
                        onValueChange = { remind = it },
                        label = { Text("Remind Before (minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    if (title.isBlank()) return@Button

                    val finalDue = due.trim()
                    val remindBefore = remind.toLongOrNull() ?: 10L

                    db.collection("tasks")
                        .add(
                            mapOf(
                                "title" to title.trim(),
                                "subject" to subject.trim().ifBlank { "General" },
                                "difficulty" to difficulty.trim().ifBlank { "Medium" },
                                "category" to category.trim().ifBlank { "Assignment" },
                                "due" to finalDue,
                                "remindBeforeMinutes" to remindBefore,
                                "completed" to false,
                                "createdAt" to Timestamp.now(),
                                "uid" to uid
                            )
                        )
                        .addOnSuccessListener { doc ->

                            val channelId = "task_channel"
                            val manager = context.getSystemService(NotificationManager::class.java)
                            if (manager.getNotificationChannel(channelId) == null) {
                                manager.createNotificationChannel(
                                    NotificationChannel(
                                        channelId,
                                        "Task Notifications",
                                        NotificationManager.IMPORTANCE_HIGH
                                    )
                                )
                            }

                            val notification = NotificationCompat.Builder(context, channelId)
                                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                                .setContentTitle("Task Created")
                                .setContentText("New Task: ${title.trim()}")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true)
                                .build()

                            if (
                                ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                NotificationManagerCompat.from(context)
                                    .notify(doc.id.hashCode(), notification)
                            }

                            scheduleReminder(
                                context,
                                doc.id,
                                title.trim(),
                                finalDue,
                                remindBefore
                            )

                            nav.popBackStack()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Task", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
