package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var remindBefore by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
                Text("Add Task", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Text("Tasks", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Enter task name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                placeholder = { Text("Math, Science…") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = difficulty,
                onValueChange = { difficulty = it },
                label = { Text("Difficulty") },
                placeholder = { Text("Easy / Medium / Hard") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                placeholder = { Text("Assignment / Exam / Project") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = due,
                onValueChange = { due = it },
                label = { Text("Due") },
                placeholder = { Text("MM/dd/yyyy HH:mm") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = remindBefore,
                onValueChange = { remindBefore = it },
                label = { Text("Remind before (min)") },
                placeholder = { Text("10, 20…") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val data = mapOf(
                        "title" to title.trim(),
                        "subject" to subject.trim(),
                        "difficulty" to difficulty.trim(),
                        "category" to category.trim(),
                        "due" to due.trim(),
                        "remindBeforeMinutes" to (remindBefore.toLongOrNull() ?: 0L),
                        "completed" to false,
                        "createdAt" to Timestamp.now()
                    )

                    db.collection("users").document(uid)
                        .collection("tasks")
                        .add(data)

                    nav.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
