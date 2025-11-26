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
import com.example.focusticks.ai.GeminiApi
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    var extractedTasks by remember { mutableStateOf(listOf<AiTaskEditable>()) }
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
                    val aiTasks = withContext(Dispatchers.IO) {
                        GeminiApi.extractTasks(bitmap)
                    }
                    extractedTasks = aiTasks.map {
                        AiTaskEditable(
                            title = it.title,
                            subject = it.subject,
                            difficulty = it.difficulty,
                            category = it.category,
                            due = it.due,
                            reminder = "10"
                        )
                    }
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

            previewBitmap?.let {
                Spacer(Modifier.height(20.dp))
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            if (extractedTasks.isNotEmpty()) {
                LazyColumn {
                    items(extractedTasks) { t ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                OutlinedTextField(
                                    value = t.title,
                                    onValueChange = { t.title = it },
                                    label = { Text("Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = t.subject,
                                    onValueChange = { t.subject = it },
                                    label = { Text("Subject") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = t.difficulty,
                                    onValueChange = { t.difficulty = it },
                                    label = { Text("Difficulty") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = t.category,
                                    onValueChange = { t.category = it },
                                    label = { Text("Category") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = t.due,
                                    onValueChange = { t.due = it },
                                    label = { Text("Due (MM/dd/yyyy HH:mm)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = t.reminder,
                                    onValueChange = { t.reminder = it },
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
                                db.collection("tasks")
                                    .add(
                                        mapOf(
                                            "uid" to uid,
                                            "title" to t.title,
                                            "subject" to t.subject,
                                            "difficulty" to t.difficulty,
                                            "category" to t.category,
                                            "due" to t.due,
                                            "remindBeforeMinutes" to (t.reminder.toLongOrNull() ?: 10L),
                                            "createdAt" to Timestamp.now(),
                                            "completed" to false
                                        )
                                    )
                                    .addOnSuccessListener { doc ->
                                        scheduleReminder(
                                            ctx,
                                            doc.id,
                                            t.title,
                                            t.due,
                                            t.reminder.toLongOrNull() ?: 10L
                                        )
                                    }
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

data class AiTaskEditable(
    var title: String,
    var subject: String,
    var difficulty: String,
    var category: String,
    var due: String,
    var reminder: String
)

suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            android.graphics.BitmapFactory.decodeStream(stream)
        } catch (_: Exception) {
            null
        }
    }
}
