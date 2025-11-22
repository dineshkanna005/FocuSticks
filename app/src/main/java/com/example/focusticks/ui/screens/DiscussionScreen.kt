package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class Comment(
    val id: String = "",
    val text: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val ref = db.collection("discussion").document(uid)

    var description by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf(listOf<Comment>()) }
    var newComment by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        ref.addSnapshotListener { snap, _ ->
            description = snap?.getString("description") ?: ""
            comments = snap?.get("comments")?.let { list ->
                (list as List<Map<String, String>>).map {
                    Comment(it["id"] ?: "", it["text"] ?: "")
                }
            } ?: emptyList()
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                }
                Text("Discussion", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Text("Description", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    ref.set(
                        mapOf("description" to description),
                        SetOptions.merge()
                    )
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Save Description")
            }

            Spacer(Modifier.height(20.dp))

            Text("Comments", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(comments) { c ->
                    CommentCard(
                        comment = c,
                        onDelete = {
                            ref.update(
                                "comments",
                                FieldValue.arrayRemove(
                                    mapOf("id" to c.id, "text" to c.text)
                                )
                            )
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Text("Add a comment", style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = newComment,
                onValueChange = { newComment = it },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (newComment.isNotBlank()) {
                        val commentObj = mapOf(
                            "id" to System.currentTimeMillis().toString(),
                            "text" to newComment.trim()
                        )
                        ref.update("comments", FieldValue.arrayUnion(commentObj))
                        newComment = ""
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Post")
            }
        }
    }
}

@Composable
fun CommentCard(comment: Comment, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comment.text,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}
