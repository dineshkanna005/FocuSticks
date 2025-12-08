package com.example.focusticks.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
    var urgency by remember { mutableStateOf(task.urgency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        title = {
            Text("Edit Task", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    label = { Text("Difficulty") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = due,
                    onValueChange = { due = it },
                    label = { Text("Due (MM/DD/YYYY HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Urgency Level", style = MaterialTheme.typography.titleMedium)

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(
                        selected = urgency == "gentle",
                        onClick = { urgency = "gentle" }
                    )
                    Text("Gentle (24 hours)")
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(
                        selected = urgency == "moderate",
                        onClick = { urgency = "moderate" }
                    )
                    Text("Moderate (3 hours)")
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(
                        selected = urgency == "urgent",
                        onClick = { urgency = "urgent" }
                    )
                    Text("Urgent (multiple)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        task.copy(
                            title = title,
                            subject = subject,
                            difficulty = difficulty,
                            category = category,
                            due = due,
                            urgency = urgency
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
