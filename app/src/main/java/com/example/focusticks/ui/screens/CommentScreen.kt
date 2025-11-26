package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.DiscussionItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(nav: NavHostController, descriptionId: String) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""
    var comments by remember { mutableStateOf(listOf<DiscussionItem>()) }
    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<DiscussionItem?>(null) }
    var editTarget by remember { mutableStateOf<DiscussionItem?>(null) }

    LaunchedEffect(Unit) {
        db.collection("discussion").document(descriptionId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                comments = snap?.documents?.mapNotNull { d ->
                    d.toObject(DiscussionItem::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
    }

    fun postComment() {
        if (input.isBlank()) return
        if (editTarget == null) {
            db.collection("users").document(uid).get().addOnSuccessListener { u ->
                val name = u.getString("name") ?: "User"
                val data = DiscussionItem(
                    topicId = descriptionId,
                    text = input.trim(),
                    uid = uid,
                    userName = name,
                    timestamp = Timestamp.now(),
                    replyToId = replyTo?.id
                )
                db.collection("discussion").document(descriptionId)
                    .collection("comments")
                    .add(data)
                input = ""
                replyTo = null
            }
        } else {
            db.collection("discussion").document(descriptionId)
                .collection("comments").document(editTarget!!.id)
                .set(
                    mapOf(
                        "text" to input.trim(),
                        "edited" to true,
                        "editedAt" to Timestamp.now(),
                        "uid" to editTarget!!.uid,
                        "userName" to editTarget!!.userName,
                        "timestamp" to editTarget!!.timestamp,
                        "replyToId" to editTarget!!.replyToId
                    ),
                    SetOptions.merge()
                )
            input = ""
            editTarget = null
        }
    }

    fun deleteComment(id: String) {
        db.collection("discussion").document(descriptionId)
            .collection("comments")
            .document(id)
            .delete()
    }

    @Composable
    fun RenderComment(item: DiscussionItem, depth: Int) {
        val replies = comments.filter { it.replyToId == item.id }
        val isMe = item.uid == uid
        val df = SimpleDateFormat("MMM dd, HH:mm", Locale.US)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 20).dp, top = 8.dp, bottom = 8.dp)
        ) {

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {

                    Text(
                        if (isMe) "${item.userName} (Me)" else item.userName,
                        color = Color(0xFF1976D2),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(item.text)

                    Spacer(Modifier.height(4.dp))

                    item.timestamp?.let {
                        Text(
                            df.format(it.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {

                        Text(
                            "Reply",
                            color = Color(0xFF1976D2),
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable {
                                    replyTo = item
                                    editTarget = null
                                }
                        )

                        if (isMe) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        input = item.text
                                        editTarget = item
                                        replyTo = null
                                    }
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        deleteComment(item.id)
                                    }
                            )
                        }
                    }
                }
            }

            replies.forEach { reply ->
                RenderComment(reply, depth + 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comments") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(comments.filter { it.replyToId == null }) { parent ->
                    RenderComment(parent, 0)
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = {
                        Text(
                            when {
                                editTarget != null -> "Edit your comment"
                                replyTo != null -> "Reply to ${replyTo!!.userName}"
                                else -> "Add a comment"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { postComment() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        when {
                            editTarget != null -> "Update"
                            else -> "Post"
                        }
                    )
                }
            }
        }
    }
}
