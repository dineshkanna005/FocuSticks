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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTaskScreen(
    nav: NavHostController,
    openTaskId: String?,
    openType: String?,
    openDrawer: () -> Unit
) {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }
    val listState = rememberLazyListState()

    var flashId by remember { mutableStateOf(openTaskId ?: "") }
    var pendingScrollId by remember { mutableStateOf(if (openType == "completed") openTaskId else "") }

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
            .whereEqualTo("completed", true)
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
                        completed = true,
                        completedAt = d.getString("completedAt") ?: ""
                    )
                } ?: emptyList()
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
                    Text("Completed Tasks", style = MaterialTheme.typography.titleLarge)
                }
                Icon(
                    Icons.Filled.Menu,
                    null,
                    modifier = Modifier.clickable { openDrawer() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { pad ->

        LazyColumn(
            modifier = Modifier.padding(pad).padding(horizontal = 16.dp),
            state = listState
        ) {
            items(tasks) { t ->

                val index = tasks.indexOf(t)

                val elevation by animateFloatAsState(
                    targetValue = if (flashId == t.id) 10.dp.value else 3.dp.value,
                    animationSpec = tween(300),
                    label = ""
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (flashId == t.id) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(elevation.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("${index + 1}. ${t.title}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Subject: ${t.subject}", color = Color.Gray)
                        Text("Category: ${t.category}", color = Color.Gray)
                        Text("Difficulty: ${t.difficulty}", color = Color.Gray)
                        Text("Due: ${t.due}", color = Color.Gray)
                        Text("Urgency: ${t.urgency.replaceFirstChar { it.uppercase() }}", color = Color.Gray)
                        Text("Completed: ${t.completedAt}", color = Color.Gray)
                    }
                }
            }
        }
    }
}