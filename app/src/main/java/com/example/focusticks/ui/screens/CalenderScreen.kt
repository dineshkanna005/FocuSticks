package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.focusticks.ui.screens.task.TaskItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    nav: NavHostController,
    openDrawer: () -> Unit   // 🔥 added for drawer
) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""

    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    val calendar = remember { Calendar.getInstance() }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)

    LaunchedEffect(Unit) {
        db.collection("tasks")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { snap ->
                tasks = snap.documents.mapNotNull { d ->
                    val due = d.getString("due") ?: ""
                    val dueOnly = due.split(" ").firstOrNull() ?: due
                    TaskItem(
                        id = d.id,
                        title = d.getString("title") ?: "",
                        subject = d.getString("subject") ?: "",
                        category = d.getString("category") ?: "",
                        difficulty = d.getString("difficulty") ?: "",
                        due = dueOnly,
                        remindBefore = d.getLong("remindBeforeMinutes") ?: 0L,
                        completed = d.getBoolean("completed") ?: false,
                        completedAt = d.getString("completedAt") ?: ""
                    )
                }
            }
    }

    fun getDaysOfMonth(month: Int, year: Int): List<Date> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val days = mutableListOf<Date>()
        val max = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..max) {
            cal.set(year, month, i)
            days.add(cal.time)
        }
        return days
    }

    val days = getDaysOfMonth(month, year)
    val monthName = SimpleDateFormat("MMMM", Locale.US).format(
        Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
            set(Calendar.YEAR, year)
        }.time
    )

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
                    Text("Calendar", style = MaterialTheme.typography.headlineSmall)
                }

                IconButton(onClick = { openDrawer() }) {
                    Icon(Icons.Filled.Menu, null)
                }
            }
        }
    ) { pad ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "<",
                    fontSize = 26.sp,
                    modifier = Modifier.clickable {
                        if (month == 0) {
                            month = 11
                            year--
                        } else month--
                    }
                )
                Text("$monthName $year", fontSize = 22.sp)
                Text(
                    ">",
                    fontSize = 26.sp,
                    modifier = Modifier.clickable {
                        if (month == 11) {
                            month = 0
                            year++
                        } else month++
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                gridItems(days) { date ->

                    val isToday = sdf.format(date) == sdf.format(Date())
                    val dayString = SimpleDateFormat("d", Locale.US).format(date)
                    val hasTasks = tasks.any { it.due == sdf.format(date) }

                    Column(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDate = date },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(dayString, fontSize = 16.sp)
                        if (hasTasks) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            selectedDate?.let {
                val dateString = sdf.format(it)
                val dayTasks = tasks.filter { t -> t.due == dateString }

                Text("Tasks for $dateString", fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(dayTasks) { t ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(t.title, fontSize = 18.sp)
                                Text(t.subject, fontSize = 14.sp, color = Color.Gray)
                                Text("Due: ${t.due}", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
