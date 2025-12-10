package com.example.focusticks.ui.screens.task

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

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
    var fileUrl by remember { mutableStateOf(task.fileUrl) }
    var imageUrl by remember { mutableStateOf(task.imageUrl) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }

    val storage = Firebase.storage

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        newImageUri = uri
    }

    fun uploadImageThenSave() {
        if (newImageUri == null) {
            onSave(
                task.copy(
                    title = title,
                    subject = subject,
                    difficulty = difficulty,
                    category = category,
                    due = due,
                    urgency = urgency,
                    imageUrl = imageUrl,
                    fileUrl = fileUrl
                )
            )
            return
        }

        uploading = true
        val ref = storage.reference.child("taskImages/${System.currentTimeMillis()}.jpg")
        ref.putFile(newImageUri!!).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                imageUrl = url.toString()
                uploading = false
                onSave(
                    task.copy(
                        title = title,
                        subject = subject,
                        difficulty = difficulty,
                        category = category,
                        due = due,
                        urgency = urgency,
                        imageUrl = imageUrl,
                        fileUrl = fileUrl
                    )
                )
            }
        }
    }

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

                Button(
                    onClick = { pickImage.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (newImageUri != null) "Image Selected" else "Change Image")
                }

                OutlinedTextField(
                    value = fileUrl,
                    onValueChange = { fileUrl = it },
                    label = { Text("File/PDF Link") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { uploadImageThenSave() },
                shape = RoundedCornerShape(12.dp),
                enabled = !uploading
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                enabled = !uploading
            ) {
                Text("Cancel")
            }
        }
    )
}
