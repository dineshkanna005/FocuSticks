package com.example.focusticks.ui.screens.discussion

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(nav: NavHostController, groupId: String) {

    val db = Firebase.firestore
    val storage = Firebase.storage
    val uid = Firebase.auth.currentUser?.uid ?: ""

    var messages by remember { mutableStateOf(listOf<GroupMessage>()) }
    var input by remember { mutableStateOf("") }
    var replyMessage by remember { mutableStateOf<GroupMessage?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var showOptionsFor by remember { mutableStateOf<GroupMessage?>(null) }
    var editModeMessageId by remember { mutableStateOf<String?>(null) }
    var groupName by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { picked -> if (picked != null) imageUri = picked }

    LaunchedEffect(Unit) {
        db.collection("groups").document(groupId)
            .get().addOnSuccessListener {
                groupName = it.getString("name") ?: "Discussion"
            }

        db.collection("groups").document(groupId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                messages = snap?.documents?.mapNotNull { d ->
                    GroupMessage(
                        id = d.id,
                        uid = d.getString("uid") ?: "",
                        userName = d.getString("username") ?: "",
                        text = d.getString("text") ?: "",
                        timestamp = d.getLong("timestamp") ?: 0L,
                        imageUrl = d.getString("imageUrl")
                    )
                } ?: emptyList()
            }
    }
    fun saveMessage() {
        Firebase.firestore.collection("users").document(uid)
            .get().addOnSuccessListener { u ->

                val username = u.getString("name")?.ifBlank { "User" } ?: "User"

                if (editModeMessageId == null) {
                    val msg = mapOf(
                        "uid" to uid,
                        "username" to username,
                        "text" to input,
                        "imageUrl" to imageUrl,
                        "replyTo" to replyMessage?.id,
                        "replyText" to replyMessage?.text,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("groups").document(groupId)
                        .collection("messages")
                        .add(msg)

                } else {
                    db.collection("groups").document(groupId)
                        .collection("messages")
                        .document(editModeMessageId!!)
                        .update("text", input)

                    editModeMessageId = null
                }

                input = ""
                imageUrl = null
                imageUri = null
                replyMessage = null
            }
    }

    fun sendMessage() {
        if (input.isBlank() && imageUri == null) return

        if (imageUri != null) {
            val ref = storage.reference.child("groups/$groupId/${System.currentTimeMillis()}.jpg")
            ref.putFile(imageUri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    imageUrl = it.toString()
                    saveMessage()
                }
            }
        } else {
            saveMessage()
        }
    }

    fun deleteMessage(id: String) {
        db.collection("groups").document(groupId)
            .collection("messages")
            .document(id).delete()
    }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Text(groupName, style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { pad ->

        Column(
            modifier = Modifier.padding(pad).fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)
            ) {
                items(messages) { msg ->

                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            .clickable(enabled = msg.uid == uid) { showOptionsFor = msg },
                        horizontalAlignment =
                            if (msg.uid == uid) Alignment.End else Alignment.Start
                    ) {

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                if (msg.uid == uid)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.widthIn(max = 260.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {

                                Text(msg.userName, style = MaterialTheme.typography.labelMedium)

                                if (msg.text.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(msg.text)
                                }

                                if (msg.imageUrl != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Image(
                                        bitmap = remember(msg.imageUrl) {
                                            val stream =
                                                java.net.URL(msg.imageUrl).openStream()
                                            BitmapFactory.decodeStream(stream).asImageBitmap()
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.height(150.dp).fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Default.CameraAlt, null)
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )

                IconButton(onClick = { sendMessage() }) {
                    Icon(Icons.Default.Send, null)
                }
            }
        }
    }

    if (showOptionsFor != null) {
        AlertDialog(
            onDismissRequest = { showOptionsFor = null },
            title = { Text("Message Options") },
            text = { Text("Choose an action") },
            confirmButton = {
                TextButton(onClick = {
                    editModeMessageId = showOptionsFor!!.id
                    input = showOptionsFor!!.text
                    showOptionsFor = null
                }) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteMessage(showOptionsFor!!.id)
                    showOptionsFor = null
                }) {
                    Text("Delete")
                }
            }
        )
    }
}
