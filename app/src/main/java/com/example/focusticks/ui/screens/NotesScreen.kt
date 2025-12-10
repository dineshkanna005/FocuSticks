package com.example.focusticks.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

data class NoteItem(
    val id: String = "",
    val uid: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val files: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val storage = Firebase.storage
    val uid = Firebase.auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    var notes by remember { mutableStateOf(listOf<NoteItem>()) }
    var search by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogBody by remember { mutableStateOf("") }
    var dialogFileLink by remember { mutableStateOf("") }
    var dialogImageUri by remember { mutableStateOf<Uri?>(null) }
    var dialogImageUrl by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }

    var previewUrl by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> dialogImageUri = uri }

    LaunchedEffect(Unit) {
        db.collection("notes")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, _ ->
                notes = snap?.documents?.mapNotNull {
                    NoteItem(
                        id = it.id,
                        uid = uid,
                        title = it.getString("title") ?: "",
                        body = it.getString("body") ?: "",
                        imageUrl = it.getString("imageUrl") ?: "",
                        files = it.get("files") as? List<String> ?: emptyList()
                    )
                } ?: emptyList()
            }
    }

    fun saveNote() {
        val fileLink = dialogFileLink.trim()
        val fileList = if (fileLink.isNotBlank()) listOf(fileLink) else emptyList()

        if (dialogImageUri != null) {
            val ref = storage.reference.child("notes/${System.currentTimeMillis()}.jpg")
            ref.putFile(dialogImageUri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    if (editingId == null) {
                        db.collection("notes").add(
                            mapOf(
                                "uid" to uid,
                                "title" to dialogTitle,
                                "body" to dialogBody,
                                "imageUrl" to url.toString(),
                                "files" to fileList
                            )
                        )
                    } else {
                        val updates = mutableMapOf<String, Any>(
                            "title" to dialogTitle,
                            "body" to dialogBody,
                            "imageUrl" to url.toString()
                        )
                        if (fileLink.isNotBlank()) updates["files"] = FieldValue.arrayUnion(fileLink)
                        db.collection("notes").document(editingId!!).update(updates)
                    }
                    dialogTitle = ""
                    dialogBody = ""
                    dialogFileLink = ""
                    dialogImageUri = null
                    dialogImageUrl = ""
                    editingId = null
                    showDialog = false
                }
            }
        } else {
            if (editingId == null) {
                db.collection("notes").add(
                    mapOf(
                        "uid" to uid,
                        "title" to dialogTitle,
                        "body" to dialogBody,
                        "imageUrl" to dialogImageUrl,
                        "files" to fileList
                    )
                )
            } else {
                val updates = mutableMapOf<String, Any>(
                    "title" to dialogTitle,
                    "body" to dialogBody,
                    "imageUrl" to dialogImageUrl
                )
                if (fileLink.isNotBlank()) updates["files"] = FieldValue.arrayUnion(fileLink)
                db.collection("notes").document(editingId!!).update(updates)
            }
            dialogTitle = ""
            dialogBody = ""
            dialogFileLink = ""
            dialogImageUri = null
            editingId = null
            showDialog = false
        }
    }

    fun openFile(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    fun deleteNote(id: String, imageUrl: String) {
        if (imageUrl.isNotBlank()) {
            Firebase.storage.getReferenceFromUrl(imageUrl).delete()
        }
        db.collection("notes").document(id).delete()
    }

    fun deleteFile(noteId: String, url: String) {
        db.collection("notes").document(noteId)
            .update("files", FieldValue.arrayRemove(url))
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
                    Text("Notes", style = MaterialTheme.typography.headlineSmall)
                }
                IconButton(onClick = { openDrawer() }) {
                    Icon(Icons.Filled.Menu, null)
                }
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize()
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search notes") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    editingId = null
                    dialogTitle = ""
                    dialogBody = ""
                    dialogFileLink = ""
                    dialogImageUri = null
                    dialogImageUrl = ""
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Add Note", fontSize = 16.sp)
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val filtered = notes.filter {
                    it.title.contains(search, true) || it.body.contains(search, true)
                }

                items(filtered, key = { it.id }) { note ->
                    val isExpanded = expanded == note.id

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = if (isExpanded) "" else note.id },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    note.title.ifBlank { "Untitled Note" },
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Row {
                                    IconButton(onClick = {
                                        editingId = note.id
                                        dialogTitle = note.title
                                        dialogBody = note.body
                                        dialogFileLink = ""
                                        dialogImageUrl = note.imageUrl
                                        dialogImageUri = null
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { deleteNote(note.id, note.imageUrl) }) {
                                        Icon(Icons.Filled.Delete, null, tint = Color.Red)
                                    }
                                }
                            }

                            if (isExpanded) {
                                Spacer(Modifier.height(8.dp))

                                Text(note.body, fontSize = 15.sp, color = Color.DarkGray)

                                if (note.imageUrl.isNotBlank()) {
                                    Spacer(Modifier.height(16.dp))
                                    Image(
                                        painter = rememberAsyncImagePainter(note.imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clickable { previewUrl = note.imageUrl }
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                if (note.files.isNotEmpty()) {
                                    note.files.forEach { url ->
                                        Button(
                                            onClick = { openFile(url) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("View", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingId == null) "Add Note" else "Edit Note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = dialogTitle,
                        onValueChange = { dialogTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dialogBody,
                        onValueChange = { dialogBody = it },
                        label = { Text("Body") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 4
                    )
                    Spacer(Modifier.height(12.dp))

                    TextButton(onClick = { picker.launch("image/*") }) {
                        Text("Upload Image")
                    }

                    if (dialogImageUri != null) Text("Image Selected")
                    else if (dialogImageUrl.isNotBlank()) Text("Current Image")

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = dialogFileLink,
                        onValueChange = { dialogFileLink = it },
                        label = { Text("External File Link") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { saveNote() }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (previewUrl.isNotBlank()) {
        Dialog(onDismissRequest = { previewUrl = "" }) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(previewUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { previewUrl = "" }
                    )
                }
            }
        }
    }
}
