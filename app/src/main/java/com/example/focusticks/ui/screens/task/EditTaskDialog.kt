package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditTaskDialog(
    task: TaskItem,
    onDismiss: () -> Unit,
    onSave: (TaskItem) -> Unit
) {

    var title by remember { mutableStateOf(task.title) }
    var subject by remember { mutableStateOf(task.subject) }
    var difficulty by remember { mutableStateOf(task.difficulty) }
    var category by remember { mutableStateOf(task.category) }
    var due by remember { mutableStateOf(task.due) }
    var reminder by remember { mutableStateOf(task.remindBefore.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onSave(
                    task.copy(
                        title = title,
                        subject = subject,
                        difficulty = difficulty,
                        category = category,
                        due = due,
                        remindBefore = reminder.toLongOrNull() ?: 10L
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
        },
        title = { Text("Edit Task") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = difficulty, onValueChange = { difficulty = it }, label = { Text("Difficulty") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("Due") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = reminder, onValueChange = { reminder = it }, label = { Text("Reminder (minutes)") })
            }
        }
    )
}
