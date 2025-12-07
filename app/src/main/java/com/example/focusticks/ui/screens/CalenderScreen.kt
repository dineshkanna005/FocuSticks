package com.example.focusticks.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val tasks = remember { mutableStateListOf<TaskCal>() }

    val calendar = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableStateOf(calendar.clone() as Calendar) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showWeekView by remember { mutableStateOf(false) }

    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        db.collection("tasks")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, _ ->
                tasks.clear()
                snap?.documents?.forEach { d ->
                    tasks.add(
                        TaskCal(
                            id = d.id,
                            title = d.getString("title") ?: "",
                            subject = d.getString("subject") ?: "",
                            difficulty = d.getString("difficulty") ?: "",
                            due = d.getString("due") ?: "",
                            completed = d.getBoolean("completed") ?: false
                        )
                    )
                }
            }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text("Calendar", style = MaterialTheme.typography.headlineSmall)
                }
                IconButton(onClick = { openDrawer() }) {
                    Icon(Icons.Filled.Menu, null)
                }
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                }) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                }

                Text(
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                    fontSize = 20.sp
                )

                IconButton(onClick = {
                    currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                }) {
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { showWeekView = !showWeekView },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (showWeekView) "Switch to Month View" else "Switch to Week View")
            }

            Spacer(Modifier.height(12.dp))

            if (showWeekView) {
                WeekView(currentMonth, tasks) {
                    selectedDate = it
                    scope.launch { modalState.show() }
                }
            } else {
                MonthView(currentMonth, tasks) {
                    selectedDate = it
                    scope.launch { modalState.show() }
                }
            }

            if (modalState.isVisible) {
                ModalBottomSheet(
                    sheetState = modalState,
                    onDismissRequest = { scope.launch { modalState.hide() } }
                ) {

                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    val dayKey = sdf.format(selectedDate)

                    Text(
                        "Tasks for $dayKey",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )

                    val dayTasks = tasks.filter { it.due.startsWith(dayKey) }

                    if (dayTasks.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No tasks for this date.")
                        }
                    } else {
                        LazyColumn {
                            items(dayTasks) { t ->
                                TaskBottomRow(t) {
                                    // FIX: Navigate to the correct screen and pass the task ID
                                    val destination = if (t.completed) "task_completed" else "task"
                                    nav.navigate("$destination?openTaskId=${t.id}&openType=${if (t.completed) "completed" else "pending"}")
                                    // Hide the bottom sheet after navigating
                                    scope.launch { modalState.hide() }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

data class TaskCal(
    val id: String,
    val title: String,
    val subject: String,
    val difficulty: String,
    val due: String,
    val completed: Boolean
)

@Composable
fun TaskBottomRow(task: TaskCal, onClick: () -> Unit) {

    val color =
        if (task.completed) Color(0xFF4CAF50)
        else {
            val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
            val dueDate = sdf.parse(task.due)
            val now = Date()
            if (dueDate != null && now.after(dueDate)) Color.Red else Color(0xFFFFA500)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(task.title, fontSize = 18.sp)
                Text(task.subject, fontSize = 14.sp, color = Color.Gray)
            }
            Box(
                Modifier.size(16.dp).background(color, MaterialTheme.shapes.small)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthView(
    month: Calendar,
    tasks: List<TaskCal>,
    onClick: (Date) -> Unit
) {
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val start = cal.get(Calendar.DAY_OF_WEEK) - 1
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val list = buildList {
        repeat(start) { add(null) }
        for (d in 1..maxDay) {
            val c = month.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, d)
            add(c.time)
        }
    }

    LazyVerticalGrid(columns = GridCells.Fixed(7)) {
        items(list.size) { i ->
            DateCell(list[i], tasks, onClick)
        }
    }
}

@Composable
fun WeekView(month: Calendar, tasks: List<TaskCal>, onClick: (Date) -> Unit) {
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val days = List(7) {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, it)
        c.time
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { d ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                DateCell(d, tasks, onClick)
            }
        }
    }
}

@Composable
fun DateCell(
    date: Date?,
    tasks: List<TaskCal>,
    onClick: (Date) -> Unit
) {
    if (date == null) {
        Box(Modifier.size(48.dp))
        return
    }

    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    val key = sdf.format(date)
    val day = SimpleDateFormat("d", Locale.getDefault()).format(date)

    val dayTasks = tasks.filter { it.due.startsWith(key) }

    val color =
        if (dayTasks.any { it.completed }) Color(0xFF4CAF50)
        else if (dayTasks.any {
                val sdfFull = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
                val due = sdfFull.parse(it.due)
                due != null && Date().after(due)
            }) Color.Red
        else if (dayTasks.isNotEmpty()) Color(0xFFFFA500)
        else Color.Transparent

    Column(
        modifier = Modifier.size(48.dp).clickable { onClick(date) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(day, fontSize = 16.sp)
        AnimatedVisibility(
            visible = dayTasks.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                Modifier.size(8.dp).background(color, MaterialTheme.shapes.small)
            )
        }
    }
}