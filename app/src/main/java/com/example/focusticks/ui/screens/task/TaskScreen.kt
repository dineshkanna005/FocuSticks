package com.example.focusticks.ui.screens.task

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.TaskReminderReceiver
import com.example.focusticks.User
import com.example.focusticks.ui.screens.task.TaskUtils.cancelAllReminders
import com.example.focusticks.ui.screens.task.TaskUtils.parseDueMillis
import com.example.focusticks.ui.screens.task.TaskUtils.scheduleMultiReminder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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
    val storage = Firebase.storage
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }
    var selectedTask by remember { mutableStateOf<TaskItem?>(null) }
    var flashId by remember { mutableStateOf(openTaskId ?: "") }
    var pendingScrollId by remember { mutableStateOf(openTaskId ?: "") }
    var sortType by remember { mutableStateOf("recent") }
    var searchQuery by remember { mutableStateOf("") }
    var uploadingTaskId by remember { mutableStateOf("") }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && uploadingTaskId.isNotEmpty()) {
            val ref = storage.reference.child("taskFiles/$uploadingTaskId/${System.currentTimeMillis()}")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    db.collection("tasks").document(uploadingTaskId)
                        .update("fileUrl", url.toString())
                    uploadingTaskId = ""
                }
            }
        }
    }

    val sortedTasks = remember(tasks, sortType, searchQuery) {
        val searched = tasks.filter {
            val s = searchQuery.trim()
            s.isBlank() ||
                    it.title.contains(s, true) ||
                    it.subject.contains(s, true) ||
                    it.category.contains(s, true)
        }
        when (sortType) {
            "easy_hard" -> searched.sortedBy { difficultyWeight(it.difficulty) }
            "hard_easy" -> searched.sortedByDescending { difficultyWeight(it.difficulty) }
            else -> searched
        }
    }

    LaunchedEffect(tasks) {
        if (tasks.isNotEmpty() && pendingScrollId.isNotEmpty()) {
            val index = tasks.indexOfFirst { it.id == pendingScrollId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                flashId = pendingScrollId
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
                        completedAt = "",
                        imageUrl = d.getString("imageUrl") ?: "",
                        fileUrl = d.getString("fileUrl") ?: ""
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

    fun unlockAchievements(uid: String, streak: Int) {
        val userRef = db.collection("users").document(uid)
        db.collection("tasks")
            .whereEqualTo("uid", uid)
            .whereEqualTo("completed", true)
            .get()
            .addOnSuccessListener { snap ->
                val total = snap.size()
                if (total >= 5) {
                    userRef.collection("achievements").document("first5")
                        .set(mapOf("title" to "First 5 Tasks Completed"))
                }
                val hard = snap.documents.count {
                    (it.getString("difficulty") ?: "").contains("hard", true)
                }
                if (hard >= 3) {
                    userRef.collection("achievements").document("hard3")
                        .set(mapOf("title" to "Completed 3 Hard Tasks in a Row"))
                }
            }
        if (streak >= 7) {
            userRef.collection("achievements").document("streak7")
                .set(mapOf("title" to "7 Day Streak"))
        }
    }

    fun markTaskAsCompleted(task: TaskItem) {
        val formatted = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US).format(Date())
        val ref = Firebase.firestore.collection("users").document(uid)
        ref.get().addOnSuccessListener { snap ->
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
            ref.update(
                mapOf(
                    "lastTaskCompleted" to now,
                    "streakDays" to newStreak,
                    "points" to newPoints
                )
            )
            unlockAchievements(uid, newStreak)
            db.collection("tasks").document(task.id)
                .update(mapOf("completed" to true, "completedAt" to formatted))
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
        scheduleMultiReminder(context, updated.id, updated.title, updated.due, updated.urgency)
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
                    Text("Tasks", style = MaterialTheme.typography.headlineSmall)
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
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { nav.navigate("scanNotes") },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.CameraAlt, null, tint = Color.White)
                }
                OutlinedIconButton(onClick = { nav.navigate("smartReminder") }) {
                    Icon(Icons.Filled.Notifications, null)
                }
                OutlinedIconButton(onClick = { nav.navigate("addTask") }) {
                    Icon(Icons.Filled.Add, null)
                }
                OutlinedIconButton(onClick = {
                    nav.navigate("task_completed?openTaskId=&openType=completed")
                }) {
                    Icon(Icons.Filled.Done, null)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Search tasks...") },
                singleLine = true
            )

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
                            onClick = { sortType = "recent"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Easy → Hard") },
                            onClick = { sortType = "easy_hard"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Hard → Easy") },
                            onClick = { sortType = "hard_easy"; expanded = false }
                        )
                    }
                }
            }

            LazyColumn(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                state = listState
            ) {
                items(sortedTasks) { t ->
                    val dueMillis = parseDueMillis(t.due)
                    val now = System.currentTimeMillis()
                    val within24 = dueMillis != null && dueMillis - now in 0..86400000
                    val overdue = dueMillis != null && now > dueMillis

                    val elevation by animateFloatAsState(
                        if (flashId == t.id) 10.dp.value else 3.dp.value,
                        tween(300)
                    )

                    val cardColor =
                        if (flashId == t.id) MaterialTheme.colorScheme.secondaryContainer
                        else if (overdue) MaterialTheme.colorScheme.errorContainer
                        else if (within24) Color(0xFFEF5350)
                        else MaterialTheme.colorScheme.surfaceVariant

                    val labelColor = Color.Black
                    val canComplete = t.imageUrl.isNotBlank() || t.fileUrl.isNotBlank()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .animateContentSize()
                            .clip(MaterialTheme.shapes.large)
                            .clickable { selectedTask = t },
                        colors = CardDefaults.cardColors(cardColor),
                        elevation = CardDefaults.cardElevation(elevation.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                        ) {
                            Column(
                                Modifier
                                    .padding(20.dp)
                            ) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("Subject: ${t.subject}", color = labelColor)
                                Text("Category: ${t.category}", color = labelColor)
                                Text("Difficulty: ${t.difficulty}", color = difficultyColor(t.difficulty))
                                Text("Timer: ${t.urgency.replaceFirstChar { it.uppercase() }}", color = labelColor)
                                Spacer(Modifier.height(12.dp))

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(t.due, color = Color.White)
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                uploadingTaskId = t.id
                                                pickFile.launch("*/*")
                                            }
                                        ) {
                                            Icon(
                                                Icons.Filled.Upload,
                                                null,
                                                modifier = Modifier.size(22.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = { if (canComplete) markTaskAsCompleted(t) },
                                            enabled = canComplete
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                null,
                                                modifier = Modifier.size(22.dp),
                                                tint = if (canComplete)
                                                    MaterialTheme.colorScheme.primary
                                                else Color.Gray
                                            )
                                        }

                                        IconButton(onClick = { selectedTask = t }) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                null,
                                                modifier = Modifier.size(22.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = { deleteTask(t.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTask?.let {
        EditTaskDialog(
            task = it,
            onDismiss = { selectedTask = null },
            onSave = ::saveTask
        )
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
