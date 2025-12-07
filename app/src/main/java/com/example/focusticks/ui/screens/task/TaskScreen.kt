package com.example.focusticks.ui.screens.task

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.TaskReminderReceiver
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val filteredTasks = remember(tasks, filterType) {
        when (filterType) {
            "easy" -> tasks.filter { it.difficulty.equals("easy", true) }
            "medium" -> tasks.filter { it.difficulty.equals("medium", true) }
            "hard" -> tasks.filter { it.difficulty.equals("hard", true) }
            else -> tasks
        }
    }

    val sortedTasks = remember(filteredTasks, sortType) {
        when (sortType) {
            "due_asc" -> filteredTasks.sortedBy { parseDueMillis(it.due) ?: Long.MAX_VALUE }
            "due_desc" -> filteredTasks.sortedByDescending { parseDueMillis(it.due) ?: Long.MIN_VALUE }
            "easy_hard" -> filteredTasks.sortedBy { it.difficulty }
            "hard_easy" -> filteredTasks.sortedByDescending { it.difficulty }
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
                        remindBefore = d.getLong("remindBeforeMinutes") ?: 0L,
                        completed = false,
                        completedAt = ""
                    )
                } ?: emptyList()
            }
    }

    fun markTaskAsCompleted(task: TaskItem) {
        val formatted = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US).format(Date())
        db.collection("tasks").document(task.id)
            .update(mapOf("completed" to true, "completedAt" to formatted))
        cancelReminder(context, task.id)
    }

    fun deleteTask(id: String) {
        db.collection("tasks").document(id).delete()
        cancelReminder(context, id)
    }

    fun saveTask(updated: TaskItem) {
        db.collection("tasks").document(updated.id)
            .set(
                mapOf(
                    "title" to updated.title,
                    "subject" to updated.subject,
                    "difficulty" to updated.difficulty,
                    "category" to updated.category,
                    "due" to updated.due,
                    "remindBeforeMinutes" to updated.remindBefore,
                    "completed" to false,
                    "uid" to uid
                )
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
                    onClick = { filterType = "hard" },
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

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recently Added") },
                            onClick = {
                                sortType = "recent"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Due Date ↑") },
                            onClick = {
                                sortType = "due_asc"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Due Date ↓") },
                            onClick = {
                                sortType = "due_desc"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Easy → Hard") },
                            onClick = {
                                sortType = "easy_hard"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hard → Easy") },
                            onClick = {
                                sortType = "hard_easy"
                                expanded = false
                            }
                        )
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

                    val dueMillis = parseDueMillis(t.due)
                    val overdue = dueMillis != null && System.currentTimeMillis() > dueMillis

                    val elevation by animateFloatAsState(
                        targetValue = if (flashId == t.id) 10.dp.value else 3.dp.value,
                        animationSpec = tween(300),
                        label = ""
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .animateContentSize()
                            .clickable { selectedTask = t },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                flashId == t.id -> MaterialTheme.colorScheme.secondaryContainer
                                overdue -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        elevation = CardDefaults.cardElevation(elevation.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    t.title,
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
                                    Text(t.due, color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text("Subject: ${t.subject}", color = Color.Gray)
                            Text("Category: ${t.category}", color = Color.Gray)
                            Text("Difficulty: ${t.difficulty}", color = Color.Gray)
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

fun cancelReminder(context: android.content.Context, taskId: String) {
    val alarm = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = android.content.Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra("taskId", taskId)
    }
    val pending = android.app.PendingIntent.getBroadcast(
        context,
        taskId.hashCode(),
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    alarm.cancel(pending)
}
