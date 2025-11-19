package com.example.focusticks.ui.screens.task

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanNotesScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedTasks by remember { mutableStateOf(listOf<ScannedTask>()) }
    var loading by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        if (uri != null) {
            scope.launch {
                loading = true
                val bitmap = loadBitmapFromUri(ctx, uri)
                previewBitmap = bitmap
                if (bitmap != null) {
                    val text = runOCR(bitmap)
                    extractedTasks = parseTasks(text)
                }
                loading = false
            }
        }
    }

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
                Text("Scan Notes", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize()
        ) {

            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose Image")
            }

            if (loading) {
                Spacer(Modifier.height(20.dp))
                Text("Processing…")
            }

            if (previewBitmap != null) {
                Spacer(Modifier.height(20.dp))
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            if (extractedTasks.isNotEmpty()) {
                LazyColumn {
                    items(extractedTasks) { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = task.title,
                                    onValueChange = { task.title = it },
                                    label = { Text("Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = task.subject,
                                    onValueChange = { task.subject = it },
                                    label = { Text("Subject") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = task.category,
                                    onValueChange = { task.category = it },
                                    label = { Text("Category") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = task.dueDate,
                                    onValueChange = { task.dueDate = it },
                                    label = { Text("Due (MM/dd/yyyy HH:mm)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = task.reminder,
                                    onValueChange = { task.reminder = it },
                                    label = { Text("Reminder (minutes)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            extractedTasks.forEach { t ->
                                db.collection("users").document(uid)
                                    .collection("tasks")
                                    .add(
                                        mapOf(
                                            "title" to t.title,
                                            "subject" to t.subject,
                                            "category" to t.category,
                                            "difficulty" to "Medium",
                                            "due" to t.dueDate,
                                            "remindBeforeMinutes" to (t.reminder.toLongOrNull() ?: 10L),
                                            "createdAt" to Timestamp.now(),
                                            "completed" to false
                                        )
                                    )
                            }
                            nav.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save All Tasks")
                }
            }
        }
    }
}

suspend fun runOCR(bitmap: Bitmap): String {
    return withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).await()
        result.text
    }
}

fun parseTasks(text: String): List<ScannedTask> {
    val lines = text.split("\n")
    val tasks = mutableListOf<ScannedTask>()

    var title = ""
    var subject = ""
    var due = ""
    var category = "Assignment"

    for (line in lines) {
        val lower = line.lowercase()
        when {
            "title" in lower -> title = line.substringAfter(":").trim()
            "subject" in lower -> subject = line.substringAfter(":").trim()
            "due" in lower -> due = line.substringAfter(":").trim()
        }

        if (title.isNotEmpty() && subject.isNotEmpty() && due.isNotEmpty()) {
            tasks.add(
                ScannedTask(
                    title = title,
                    subject = subject,
                    dueDate = due,
                    category = category,
                    reminder = "10"
                )
            )
            title = ""
            subject = ""
            due = ""
        }
    }
    return tasks
}

suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            android.graphics.BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            null
        }
    }
}

data class ScannedTask(
    var title: String = "",
    var subject: String = "",
    var dueDate: String = "",
    var category: String = "",
    var reminder: String = ""
)
