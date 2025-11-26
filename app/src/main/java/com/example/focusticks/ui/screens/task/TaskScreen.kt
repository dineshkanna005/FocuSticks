package com.example.focusticks.ui.screens.task

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.NotificationHelper
import com.example.focusticks.TaskReminderReceiver
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(nav: NavHostController, openTaskId: String?, openType: String?) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }
    var selectedTask by remember { mutableStateOf<TaskItem?>(null) }
    var flashId by remember { mutableStateOf("") }
    var scrollIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(openTaskId) {
        if (!openTaskId.isNullOrEmpty() && openType != "completed") {
            flashId = openTaskId
        }
    }

    LaunchedEffect(flashId) {
        if (flashId.isNotEmpty()) {
            kotlinx.coroutines.delay(1000)
            flashId = ""
        }
    }

    LaunchedEffect(Unit) {
        db.collection("tasks")
            .whereEqualTo("uid", uid)
            .whereEqualTo("completed", false)
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
                        completed = false,
                        completedAt = ""
                    )
                } ?: emptyList()

                if (!openTaskId.isNullOrEmpty()) {
                    scrollIndex = tasks.indexOfFirst { it.id == openTaskId }
                }
            }
    }

    LaunchedEffect(scrollIndex) {
        if (scrollIndex >= 0) {
            listState.animateScrollToItem(scrollIndex)
            scrollIndex = -1
        }
    }

    fun markTaskAsCompleted(task: TaskItem) {
        val now = System.currentTimeMillis()
        val nowFormatted = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US).format(Date())

        db.collection("tasks").document(task.id)
            .update(
                mapOf(
                    "completed" to true,
                    "completedAt" to nowFormatted
                )
            )
            .addOnSuccessListener {
                flashId = task.id

                db.collection("users").document(uid)
                    .update(
                        mapOf(
                            "points" to FieldValue.increment(10L),
                            "lastTaskCompleted" to now
                        )
                    )

                NotificationHelper.showReminderNotification(
                    context,
                    "Task Completed: ${task.title}",
                    task.id,
                    "completed"
                )
            }

        cancelReminder(context, task.id)
    }

    fun deleteTask(taskId: String) {
        db.collection("tasks").document(taskId).delete()
        cancelReminder(context, taskId)
    }

    fun saveTask(task: TaskItem) {
        db.collection("tasks").document(task.id)
            .set(
                mapOf(
                    "title" to task.title,
                    "subject" to task.subject,
                    "difficulty" to task.difficulty,
                    "category" to task.category,
                    "due" to task.due,
                    "remindBeforeMinutes" to task.remindBefore,
                    "completed" to false,
                    "uid" to uid
                )
            )

        val dueMillis = parseDueMillis(task.due)
        if (dueMillis != null) {
            scheduleReminder(context, task.id, task.title, task.due, task.remindBefore)
        } else {
            cancelReminder(context, task.id)
        }

        selectedTask = null
    }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.clickable { nav.popBackStack() }
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("Task", style = MaterialTheme.typography.headlineMedium)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scan Notes", modifier = Modifier.clickable { nav.navigate("scanNotes") })
                    Text("Smart Reminder", modifier = Modifier.clickable { nav.navigate("smartReminder") })
                    Text("Add", modifier = Modifier.clickable { nav.navigate("addTask") })
                    Text("Completed", modifier = Modifier.clickable { nav.navigate("task_completed") })
                }
            }
        }
    ) { pad ->

        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            state = listState
        ) {
            items(tasks) { t ->

                val dueMillis = parseDueMillis(t.due)
                val isOverdue = dueMillis != null && System.currentTimeMillis() > dueMillis

                val animatedElevation by animateFloatAsState(
                    targetValue = if (flashId == t.id) 8.dp.value else 4.dp.value,
                    animationSpec = tween(300),
                    label = ""
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { selectedTask = t },
                    elevation = CardDefaults.cardElevation(animatedElevation.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            flashId == t.id -> MaterialTheme.colorScheme.secondaryContainer
                            isOverdue -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Subject: ${t.subject}", style = MaterialTheme.typography.bodySmall)
                            Text("Due: ${t.due}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { markTaskAsCompleted(t) }) {
                                Icon(Icons.Filled.Check, null)
                            }
                            IconButton(onClick = { selectedTask = t }) {
                                Icon(Icons.Filled.Edit, null)
                            }
                            IconButton(onClick = { deleteTask(t.id) }) {
                                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { selectedTask = null },
            onSave = ::saveTask
        )
    }
}

fun cancelReminder(context: android.content.Context, taskId: String) {
    val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = android.content.Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra("taskId", taskId)
    }
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context,
        taskId.hashCode(),
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}
