package com.example.focusticks.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
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
    val files: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val storage = Firebase.storage
    val uid = Firebase.auth.currentUser?.uid ?: ""

    var notes by remember { mutableStateOf(listOf<NoteItem>()) }
    var search by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogBody by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val noteId = expanded
        if (noteId.isNotEmpty()) {
            uris.forEach { uri ->
                val ref = storage.reference.child("notes/$uid/$noteId/${System.currentTimeMillis()}")
                ref.putFile(uri).addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        db.collection("notes").document(noteId)
                            .update("files", FieldValue.arrayUnion(url.toString()))
                    }
                }
            }
        }
    }

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
                        files = it.get("files") as? List<String> ?: emptyList()
                    )
                } ?: emptyList()
            }
    }

    fun saveNote() {
        if (dialogTitle.isBlank() && dialogBody.isBlank()) return
        if (editingId == null) {
            db.collection("notes").add(
                mapOf(
                    "uid" to uid,
                    "title" to dialogTitle,
                    "body" to dialogBody,
                    "files" to emptyList<String>()
                )
            )
        } else {
            db.collection("notes").document(editingId!!)
                .update(mapOf("title" to dialogTitle, "body" to dialogBody))
        }
        dialogTitle = ""
        dialogBody = ""
        editingId = null
        showDialog = false
    }

    fun openFile(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { nav.context.startActivity(intent) } catch (_: Exception) {}
    }

    fun deleteNote(id: String) {
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

                items(notes.filter {
                    it.title.contains(search, true) || it.body.contains(search, true)
                }, key = { it.id }) { note ->

                    val isExpanded = expanded == note.id

                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { expanded = if (isExpanded) "" else note.id },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {

                        Column(Modifier.padding(16.dp)) {

                            Text(
                                note.title.ifBlank { "Untitled Note" },
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isExpanded) {

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    note.body,
                                    fontSize = 15.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(Modifier.height(12.dp))

                                if (note.files.isNotEmpty()) {
                                    Text("Attachments:", fontSize = 14.sp)
                                    Spacer(Modifier.height(6.dp))

                                    note.files.forEach { url ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable { openFile(url) }
                                        ) {
                                            Text(
                                                url.substringAfterLast("/"),
                                                modifier = Modifier.weight(1f),
                                                fontSize = 13.sp
                                            )
                                            IconButton(onClick = { deleteFile(note.id, url) }) {
                                                Icon(Icons.Filled.Delete, null, tint = Color.Red)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        expanded = note.id
                                        picker.launch("*/*")
                                    }) {
                                        Icon(Icons.Filled.Upload, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        editingId = note.id
                                        dialogTitle = note.title
                                        dialogBody = note.body
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { deleteNote(note.id) }) {
                                        Icon(Icons.Filled.Delete, null, tint = Color.Red)
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
}
