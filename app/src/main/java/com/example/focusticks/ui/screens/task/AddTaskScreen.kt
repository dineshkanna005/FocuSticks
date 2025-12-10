package com.example.focusticks.ui.screens.task

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.focusticks.ui.screens.task.TaskUtils.scheduleMultiReminder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val storage = Firebase.storage
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("gentle") }
    var fileUrl by remember { mutableStateOf("") }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        pickedImageUri = uri
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
                    Text("Upload Image or File", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { pickImage.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (pickedImageUri != null) "Image Selected" else "Upload Image")
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = fileUrl,
                        onValueChange = { fileUrl = it },
                        label = { Text("Paste File/PDF Link") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))
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
                    Spacer(Modifier.height(16.dp))
                    Text("Reminder Urgency", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = urgency == "gentle", onClick = { urgency = "gentle" })
                        Text("Gentle (24 hours before)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = urgency == "moderate", onClick = { urgency = "moderate" })
                        Text("Moderate (3 hours before)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = urgency == "urgent", onClick = { urgency = "urgent" })
                        Text("Urgent (multiple reminders)")
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    if (uploading) return@Button

                    uploading = true
                    val finalDue = due.trim()

                    if (pickedImageUri != null) {
                        val ref = storage.reference.child("taskImages/${System.currentTimeMillis()}.jpg")
                        ref.putFile(pickedImageUri!!).addOnSuccessListener {
                            ref.downloadUrl.addOnSuccessListener { url ->
                                createTask(db, uid, title, subject, difficulty, category, finalDue, urgency, url.toString(), fileUrl, context, nav)
                                uploading = false
                            }
                        }
                    } else {
                        createTask(db, uid, title, subject, difficulty, category, finalDue, urgency, "", fileUrl, context, nav)
                        uploading = false
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

fun createTask(
    db: com.google.firebase.firestore.FirebaseFirestore,
    uid: String,
    title: String,
    subject: String,
    difficulty: String,
    category: String,
    due: String,
    urgency: String,
    imageUrl: String,
    fileUrl: String,
    context: android.content.Context,
    nav: NavHostController
) {
    db.collection("tasks")
        .add(
            mapOf(
                "title" to title.trim(),
                "subject" to subject.trim().ifBlank { "General" },
                "difficulty" to difficulty.trim().ifBlank { "Medium" },
                "category" to category.trim().ifBlank { "Assignment" },
                "due" to due,
                "urgencyLevel" to urgency,
                "completed" to false,
                "createdAt" to Timestamp.now(),
                "uid" to uid,
                "imageUrl" to imageUrl,
                "fileUrl" to fileUrl.trim()
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

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(doc.id.hashCode(), notification)
            }

            scheduleMultiReminder(context, doc.id, title.trim(), due, urgency)
            nav.popBackStack()
        }
}
