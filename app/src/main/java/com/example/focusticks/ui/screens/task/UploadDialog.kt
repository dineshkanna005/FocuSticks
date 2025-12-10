package com.example.focusticks.ui.screens.task

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch

@Composable
fun UploadDialog(
    taskId: String,
    onDismiss: () -> Unit,
    onUploaded: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val storage = Firebase.storage
    var uploadProgress by remember { mutableStateOf(false) }

    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImage = uri }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedFile = uri }

    AlertDialog(
        onDismissRequest = { if (!uploadProgress) onDismiss() },
        title = { Text("Upload Attachment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { pickImage.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pick Image")
                }
                Button(
                    onClick = { pickFile.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pick File / PDF")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedImage != null) {
                        uploadProgress = true
                        val ref = storage.reference.child("tasks/$taskId/image.jpg")
                        scope.launch {
                            ref.putFile(selectedImage!!).addOnSuccessListener {
                                ref.downloadUrl.addOnSuccessListener { url ->
                                    uploadProgress = false
                                    onUploaded(url.toString(), "")
                                }
                            }
                        }
                    } else if (selectedFile != null) {
                        uploadProgress = true
                        val ref = storage.reference.child("tasks/$taskId/file")
                        scope.launch {
                            ref.putFile(selectedFile!!).addOnSuccessListener {
                                ref.downloadUrl.addOnSuccessListener { url ->
                                    uploadProgress = false
                                    onUploaded("", url.toString())
                                }
                            }
                        }
                    }
                },
                enabled = !uploadProgress && (selectedImage != null || selectedFile != null)
            ) {
                Text(if (uploadProgress) "Uploading..." else "Upload")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { if (!uploadProgress) onDismiss() }
            ) {
                Text("Cancel")
            }
        }
    )
}
