package com.example.focusticks.ui.screens.task

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.TaskReminderReceiver
import com.example.focusticks.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.toObject
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    nav: NavHostController,
    openTaskId: String?,
    openType: String?,
    openDrawer: () -> Unit
) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }
    var selectedTask by remember { mutableStateOf<TaskItem?>(null) }

    var flashId by remember { mutableStateOf(openTaskId ?: "") }
    var pendingScrollId by remember { mutableStateOf(openTaskId ?: "") }

    var sortType by remember { mutableStateOf("recent") }
    var filterType by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTasks = remember(tasks, filterType, searchQuery) {
        tasks.filter { task ->
            val matchDiff = when (filterType) {
                "easy" -> task.difficulty.equals("easy", true)
                "medium" -> task.difficulty.equals("medium", true)
                "hard" -> task.difficulty.equals("hard", true)
                else -> true
            }
            val q = searchQuery.trim()
            val matchSearch = q.isBlank() ||
                    task.title.contains(q, ignoreCase = true) ||
                    task.subject.contains(q, ignoreCase = true) ||
                    task.category.contains(q, ignoreCase = true)
            matchDiff && matchSearch
        }
    }

    val sortedTasks = remember(filteredTasks, sortType) {
        when (sortType) {
            "due_asc" -> filteredTasks.sortedBy { parseDueMillis(it.due) ?: Long.MAX_VALUE }
            "due_desc" -> filteredTasks.sortedByDescending { parseDueMillis(it.due) ?: Long.MIN_VALUE }
            "easy_hard" -> filteredTasks.sortedBy { difficultyWeight(it.difficulty) }
            "hard_easy" -> filteredTasks.sortedByDescending { difficultyWeight(it.difficulty) }
            else -> filteredTasks
        }
    }

    LaunchedEffect(tasks) {
        if (tasks.isNotEmpty() && !pendingScrollId.isNullOrEmpty()) {
            val index = tasks.indexOfFirst { it.id == pendingScrollId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                flashId = pendingScrollId!!
                pendingScrollId = ""
            }
        }
    }

    LaunchedEffect(flashId) {
        if (flashId.isNotEmpty()) {
            delay(900)
            flashId = ""
        }
    }

    LaunchedEffect(Unit) {
        db.collection("tasks")
            .whereEqualTo("uid", uid)
            .whereEqualTo("completed", false)
            .addSnapshotListener { snap, _ ->
                tasks = snap?.documents?.map { d ->
                    TaskItem(
                        id = d.id,
                        title = d.getString("title") ?: "",
                        subject = d.getString("subject") ?: "",
                        category = d.getString("category") ?: "",
                        difficulty = d.getString("difficulty") ?: "",
                        due = d.getString("due") ?: "",
                        urgency = d.getString("urgencyLevel") ?: "gentle",
                        completed = false,
                        completedAt = ""
                    )
                } ?: emptyList()
            }
    }

    fun sendCompletedNotification(context: android.content.Context, task: TaskItem) {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra("taskId", task.id)
            putExtra("title", "Task Completed: ${task.title}")
            putExtra("urgency", "completed")
        }
        context.sendBroadcast(intent)
    }

    fun markTaskAsCompleted(task: TaskItem) {
        val formatted = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US).format(Date())
        val usersRef = Firebase.firestore.collection("users").document(uid)

        usersRef.get().addOnSuccessListener { snap ->
            val user = snap.toObject<User>() ?: User(uid = uid)

            val now = System.currentTimeMillis()
            val lastDay = TimeUnit.MILLISECONDS.toDays(user.lastTaskCompleted)
            val todayDay = TimeUnit.MILLISECONDS.toDays(now)

            val newStreak =
                if (user.lastTaskCompleted == 0L) 1
                else if (todayDay == lastDay) user.streakDays
                else if (todayDay - lastDay == 1L) user.streakDays + 1
                else 1

            val newPoints = user.points + 10

            usersRef.update(
                mapOf(
                    "lastTaskCompleted" to now,
                    "streakDays" to newStreak,
                    "points" to newPoints
                )
            )

            db.collection("tasks").document(task.id)
                .update(
                    mapOf(
                        "completed" to true,
                        "completedAt" to formatted
                    )
                )

            cancelAllReminders(context, task.id)
            sendCompletedNotification(context, task)
        }
    }

    fun deleteTask(id: String) {
        db.collection("tasks").document(id).delete()
        cancelAllReminders(context, id)
    }

    fun saveTask(updated: TaskItem) {
        db.collection("tasks").document(updated.id)
            .update(
                mapOf(
                    "title" to updated.title,
                    "subject" to updated.subject,
                    "difficulty" to updated.difficulty,
                    "category" to updated.category,
                    "due" to updated.due,
                    "urgencyLevel" to updated.urgency
                )
            )
        cancelAllReminders(context, updated.id)
        scheduleMultiReminder(
            context,
            updated.id,
            updated.title,
            updated.due,
            updated.urgency
        )
        selectedTask = null
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
                    Text("Task", style = MaterialTheme.typography.headlineSmall)
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
        Column(Modifier.padding(pad)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Scan", Modifier.clickable { nav.navigate("scanNotes") })
                Text("Smart Reminder", Modifier.clickable { nav.navigate("smartReminder") })
                Text("Add", Modifier.clickable { nav.navigate("addTask") })
                Text("Completed", Modifier.clickable { nav.navigate("task_completed") })
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Search by title, subject, or category") },
                singleLine = true
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                FilterChip(
                    selected = filterType == "all",
                    onClick = { filterType = "all" },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = filterType == "easy",
                    onClick = { filterType = "easy" },
                    label = { Text("Easy") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = filterType == "medium",
                    onClick = { filterType = "medium" },
                    label = { Text("Medium") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = filterType == "hard",
                    onClick = { filterType == "hard" },
                    label = { Text("Hard") }
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expanded = true }) {
                        Text(
                            when (sortType) {
                                "due_asc" -> "Due ↑"
                                "due_desc" -> "Due ↓"
                                "easy_hard" -> "Easy → Hard"
                                "hard_easy" -> "Hard → Easy"
                                else -> "Recent"
                            }
                        )
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Recently Added") }, onClick = {
                            sortType = "recent"
                            expanded = false
                        })
                        DropdownMenuItem(text = { Text("Due Date ↑") }, onClick = {
                            sortType = "due_asc"; expanded = false
                        })
                        DropdownMenuItem(text = { Text("Due Date ↓") }, onClick = {
                            sortType = "due_desc"; expanded = false
                        })
                        DropdownMenuItem(text = { Text("Easy → Hard") }, onClick = {
                            sortType = "easy_hard"; expanded = false
                        })
                        DropdownMenuItem(text = { Text("Hard → Easy") }, onClick = {
                            sortType = "hard_easy"; expanded = false
                        })
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                state = listState
            ) {
                items(sortedTasks) { t ->
                    val index = sortedTasks.indexOf(t)
                    val dueMillis = parseDueMillis(t.due)
                    val overdue = dueMillis != null && System.currentTimeMillis() > dueMillis
                    val dueToday = isDueToday(t.due)

                    val elevation by animateFloatAsState(
                        targetValue = if (flashId == t.id) 10.dp.value else 3.dp.value,
                        animationSpec = tween(300), label = ""
                    )

                    val cardColor = when {
                        flashId == t.id -> MaterialTheme.colorScheme.secondaryContainer
                        overdue -> MaterialTheme.colorScheme.errorContainer
                        dueToday -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .animateContentSize()
                            .clickable { selectedTask = t },
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(elevation.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}. ${t.title}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Box(
                                    Modifier
                                        .background(
                                            if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        t.due,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text("Subject: ${t.subject}", color = Color.Gray)
                            Text("Category: ${t.category}", color = Color.Gray)
                            Text(
                                "Difficulty: ${t.difficulty.replaceFirstChar { it.uppercase() }}",
                                color = difficultyColor(t.difficulty)
                            )
                            Text("Urgency: ${t.urgency.replaceFirstChar { it.uppercase() }}")

                            Spacer(Modifier.height(12.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { markTaskAsCompleted(t) }) {
                                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
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
    }

    selectedTask?.let {
        EditTaskDialog(task = it, onDismiss = { selectedTask = null }, onSave = ::saveTask)
    }
}

fun cancelAllReminders(context: android.content.Context, taskId: String) {
    val alarm = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
    val ids = listOf("${taskId}_g", "${taskId}_m", "${taskId}_u1", "${taskId}_u2", "${taskId}_u3")
    ids.forEach { id ->
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pending = android.app.PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarm.cancel(pending)
    }
}

fun difficultyColor(diff: String): Color =
    when (diff.lowercase(Locale.getDefault())) {
        "easy" -> Color(0xFF4CAF50)
        "medium" -> Color(0xFFFFC107)
        "hard" -> Color(0xFFF44336)
        else -> Color.Gray
    }

fun difficultyWeight(diff: String): Int =
    when (diff.lowercase(Locale.getDefault())) {
        "easy" -> 0
        "medium" -> 1
        "hard" -> 2
        else -> 1
    }

fun isDueToday(due: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US)
        val date = sdf.parse(due) ?: return false
        val calDue = Calendar.getInstance().apply { time = date }
        val calNow = Calendar.getInstance()
        calDue.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDue.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
    } catch (e: Exception) {
        false
    }
}
