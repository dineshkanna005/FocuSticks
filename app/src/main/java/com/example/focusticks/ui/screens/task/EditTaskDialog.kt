package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: TaskItem,
    onDismiss: () -> Unit,
    onSave: (TaskItem) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var subject by remember { mutableStateOf(task.subject) }
    var category by remember { mutableStateOf(task.category) }
    var difficulty by remember { mutableStateOf(task.difficulty) }
    var due by remember { mutableStateOf(task.due) }
    var remindBefore by remember { mutableStateOf(task.remindBeforeMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    label = { Text("Difficulty") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = due,
                    onValueChange = { due = it },
                    label = { Text("Due") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = remindBefore,
                    onValueChange = { remindBefore = it },
                    label = { Text("Remind Before (min)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    TaskItem(
                        id = task.id,
                        title = title,
                        subject = subject,
                        category = category,
                        difficulty = difficulty,
                        due = due,
                        remindBeforeMinutes = remindBefore.toLongOrNull() ?: 0L,
                        completed = task.completed
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
