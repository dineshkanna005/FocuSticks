package com.example.focusticks.ui.screens.posts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCommentsScreen(nav: NavHostController, postId: String, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""
    var commentText by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf(listOf<CommentItem>()) }
    var username by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editCommentText by remember { mutableStateOf("") }
    var editingCommentId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener {
                username = it.getString("username")
                    ?: it.getString("name")
                            ?: uid.takeLast(5)
            }

        db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                comments = snap?.documents?.map {
                    CommentItem(
                        id = it.id,
                        uid = it.getString("uid") ?: "",
                        username = it.getString("username") ?: "",
                        text = it.getString("text") ?: "",
                        time = it.getLong("time") ?: 0L
                    )
                } ?: emptyList()
            }
    }

    fun sendComment() {
        if (commentText.isBlank()) return
        val data = mapOf(
            "uid" to uid,
            "username" to username,
            "text" to commentText,
            "time" to System.currentTimeMillis()
        )
        db.collection("posts").document(postId)
            .collection("comments")
            .add(data)
        commentText = ""
    }

    fun deleteComment(commentId: String) {
        db.collection("posts")
            .document(postId)
            .collection("comments")
            .document(commentId)
            .delete()
    }

    fun saveEditedComment() {
        if (editingCommentId.isNotEmpty()) {
            db.collection("posts")
                .document(postId)
                .collection("comments")
                .document(editingCommentId)
                .update("text", editCommentText)
        }
        showEditDialog = false
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text("Comments (${comments.size})", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Menu,
                    null,
                    modifier = Modifier.clickable { openDrawer() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(comments) { c ->
                    var showMenu by remember { mutableStateOf(false) }

                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            Modifier
                                .padding(16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(c.username, style = MaterialTheme.typography.titleMedium)

                                if (c.uid == uid) {
                                    Box {
                                        Icon(
                                            Icons.Filled.MoreVert,
                                            null,
                                            modifier = Modifier.clickable { showMenu = true }
                                        )
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                onClick = {
                                                    editCommentText = c.text
                                                    editingCommentId = c.id
                                                    showEditDialog = true
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = {
                                                    deleteComment(c.id)
                                                    showMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text(c.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a comment...") },
                    singleLine = true
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = { sendComment() }) {
                    Text("Send")
                }
            }
        }
    }

    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = editCommentText,
                        onValueChange = { editCommentText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { saveEditedComment() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
