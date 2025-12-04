package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTaskScreen(nav: NavHostController, openTaskId: String?, openType: String?) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var tasks by remember { mutableStateOf(listOf<TaskItem>()) }
    var flashId by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var scrollIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(openTaskId) {
        if (!openTaskId.isNullOrEmpty() && openType == "completed") flashId = openTaskId
    }

    LaunchedEffect(flashId) {
        if (flashId.isNotEmpty()) {
            kotlinx.coroutines.delay(900)
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
                        remindBefore = d.getLong("remindBeforeMinutes") ?: 0L,
                        completed = true,
                        completedAt = d.getString("completedAt") ?: ""
                    )
                } ?: emptyList()

                if (!openTaskId.isNullOrEmpty() && openType == "completed") {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Completed Tasks") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->

        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 16.dp),
            state = listState
        ) {
            items(tasks) { t ->

                val highlightColor =
                    if (flashId == t.id)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = highlightColor),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(
                        Modifier
                            .padding(18.dp)
                    ) {
                        Text(
                            t.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        )

                        Spacer(Modifier.height(6.dp))

                        Text("Subject: ${t.subject}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Category: ${t.category}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Difficulty: ${t.difficulty}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Due: ${t.due}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Completed: ${t.completedAt}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
