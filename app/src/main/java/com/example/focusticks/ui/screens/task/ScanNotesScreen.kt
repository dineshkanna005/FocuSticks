package com.example.focusticks.ui.screens.task

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavHostController
import com.example.focusticks.ai.AiTaskEditable
import com.example.focusticks.ai.OpenAIApi
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun createImageUri(context: Context): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanNotesScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore

    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val extractedTasks = remember { mutableStateListOf<AiTaskEditable>() }
    var loading by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                loading = true
                errorText = null
                val bmp = loadFullBitmap(context, uri)
                previewBitmap = bmp
                extractedTasks.clear()

                if (bmp != null) {
                    val aiTasks = OpenAIApi.extractTasks(bmp)
                    if (aiTasks.isEmpty()) {
                        extractedTasks.add(AiTaskEditable("", "", "", "", "", "10"))
                        errorText = "Could not detect tasks. Please fill manually."
                    } else extractedTasks.addAll(aiTasks)
                } else {
                    errorText = "Unable to read image."
                }
                loading = false
            }
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            scope.launch {
                loading = true
                val bmp = loadFullBitmap(context, cameraUri!!)
                previewBitmap = bmp
                extractedTasks.clear()
                if (bmp != null) {
                    val aiTasks = OpenAIApi.extractTasks(bmp)
                    if (aiTasks.isEmpty()) {
                        extractedTasks.add(AiTaskEditable("", "", "", "", "", "10"))
                    } else extractedTasks.addAll(aiTasks)
                }
                loading = false
            }
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
                    Text("Scan Notes", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                }
                Icon(Icons.Filled.Menu, "", modifier = Modifier.clickable { openDrawer() }, tint = MaterialTheme.colorScheme.primary)
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(20.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = { galleryPicker.launch("image/*") },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Gallery") }

                Button(
                    onClick = {
                        val uri = createImageUri(context)
                        if (uri != null) {
                            cameraUri = uri
                            takePhoto.launch(uri)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Camera") }
            }

            if (loading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            errorText?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            previewBitmap?.let {
                Spacer(Modifier.height(20.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Image(it.asImageBitmap(), "", Modifier.fillMaxWidth().height(260.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn(Modifier.weight(1f, false)) {
                itemsIndexed(extractedTasks) { index, t ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            OutlinedTextField(
                                value = t.title,
                                onValueChange = { extractedTasks[index] = extractedTasks[index].copy(title = it) },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = t.subject,
                                onValueChange = { extractedTasks[index] = extractedTasks[index].copy(subject = it) },
                                label = { Text("Subject") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = t.difficulty,
                                onValueChange = { extractedTasks[index] = extractedTasks[index].copy(difficulty = it) },
                                label = { Text("Difficulty") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = t.category,
                                onValueChange = { extractedTasks[index] = extractedTasks[index].copy(category = it) },
                                label = { Text("Category") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = t.due,
                                onValueChange = { extractedTasks[index] = extractedTasks[index].copy(due = it) },
                                label = { Text("Due") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = t.reminder,
                                onValueChange = { extractedTasks[index] = extractedTasks[index].copy(reminder = it) },
                                label = { Text("Reminder (minutes)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (extractedTasks.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {

                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }

                            extractedTasks.forEach { t ->
                                val reminder = t.reminder.toLongOrNull() ?: 10L

                                db.collection("tasks")
                                    .add(
                                        mapOf(
                                            "uid" to uid,
                                            "title" to t.title,
                                            "subject" to t.subject,
                                            "difficulty" to t.difficulty,
                                            "category" to t.category,
                                            "due" to t.due,
                                            "remindBeforeMinutes" to reminder,
                                            "createdAt" to Timestamp.now(),
                                            "completed" to false
                                        )
                                    )
                                    .addOnSuccessListener { doc ->

                                        val channelId = "task_channel"
                                        val manager = context.getSystemService(NotificationManager::class.java)
                                        if (manager.getNotificationChannel(channelId) == null) {
                                            manager.createNotificationChannel(
                                                NotificationChannel(
                                                    channelId,
                                                    "Task Notifications",
                                                    NotificationManager.IMPORTANCE_HIGH
                                                )
                                            )
                                        }

                                        val notification = NotificationCompat.Builder(context, channelId)
                                            .setSmallIcon(android.R.drawable.ic_popup_reminder)
                                            .setContentTitle("Task Created")
                                            .setContentText("New Task: ${t.title}")
                                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                                            .setAutoCancel(true)
                                            .build()

                                        if (ActivityCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            NotificationManagerCompat.from(context)
                                                .notify(doc.id.hashCode(), notification)
                                        }
                                    }
                            }

                            nav.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save All Tasks") }
            }
        }
    }
}

suspend fun loadFullBitmap(context: Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } catch (e: Exception) {
            null
        }
    }
}
