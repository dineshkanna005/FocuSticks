package com.example.focusticks.ui.screens.posts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsFeedScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""
    var posts by remember { mutableStateOf(listOf<PostItem>()) }
    var expandedPost by remember { mutableStateOf<String?>(null) }
    var comments by remember { mutableStateOf<Map<String, List<CommentItem>>>(emptyMap()) }
    var commentInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var editingPostId by remember { mutableStateOf("") }
    var previewUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("posts")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                posts = snap?.documents?.map {
                    PostItem(
                        id = it.id,
                        uid = it.getString("uid") ?: "",
                        username = it.getString("username") ?: "",
                        text = it.getString("text") ?: "",
                        imageUrl = it.getString("imageUrl") ?: "",
                        timestamp = it.getLong("timestamp") ?: 0L,
                        likes = (it.getLong("likes") ?: 0).toInt(),
                        likedBy = it.get("likedBy")?.let { list ->
                            (list as? List<*>)?.map { it.toString() }
                        } ?: emptyList()
                    )
                } ?: emptyList()
            }
    }

    fun toggleLike(postId: String, likedBy: List<String>) {
        if (uid.isEmpty()) return
        val ref = db.collection("posts").document(postId)
        if (likedBy.contains(uid)) {
            ref.update(
                "likedBy", FieldValue.arrayRemove(uid),
                "likes", FieldValue.increment(-1)
            )
        } else {
            ref.update(
                "likedBy", FieldValue.arrayUnion(uid),
                "likes", FieldValue.increment(1)
            )
        }
    }

    fun loadComments(postId: String) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("time")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    CommentItem(
                        id = it.id,
                        uid = it.getString("uid") ?: "",
                        username = it.getString("username") ?: "",
                        text = it.getString("text") ?: "",
                        time = it.getLong("time") ?: 0L
                    )
                } ?: emptyList()
                comments = comments.toMutableMap().apply { put(postId, list) }
            }
    }

    fun postComment(postId: String, text: String) {
        if (text.isBlank() || uid.isEmpty()) return
        Firebase.firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { userSnap ->
                val username = userSnap.getString("username")
                    ?: userSnap.getString("name")
                    ?: "Student"
                val comment = mapOf(
                    "uid" to uid,
                    "username" to username,
                    "text" to text,
                    "time" to System.currentTimeMillis()
                )
                Firebase.firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .add(comment)
                commentInputs = commentInputs.toMutableMap().apply { put(postId, "") }
            }
    }

    fun deletePost(postId: String) {
        db.collection("posts").document(postId)
            .collection("comments")
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
                db.collection("posts").document(postId).delete()
            }
    }

    fun saveEditedPost() {
        if (editingPostId.isNotEmpty()) {
            db.collection("posts").document(editingPostId)
                .update("text", editText)
        }
        showEditDialog = false
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Add,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { nav.navigate("createPost") }
                )
                Spacer(Modifier.width(12.dp))
                Text("Study Feed", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Menu,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { openDrawer() }
                )
            }
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
        ) {

            posts.forEach { p ->

                LaunchedEffect(p.id) {
                    loadComments(p.id)
                }

                val userLiked = p.likedBy.contains(uid)
                val postComments = comments[p.id] ?: emptyList()
                var showMenu by remember { mutableStateOf(false) }

                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Column(Modifier.padding(16.dp)) {

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.username, style = MaterialTheme.typography.titleMedium)

                            if (p.uid == uid) {
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
                                                editText = p.text
                                                editingPostId = p.id
                                                showEditDialog = true
                                                showMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                deletePost(p.id)
                                                showMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(p.text)

                        if (p.imageUrl.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Image(
                                painter = rememberAsyncImagePainter(p.imageUrl),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clickable { previewUrl = p.imageUrl }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text("${postComments.size} comments")

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Favorite,
                                    null,
                                    tint = if (userLiked) Color.Red else Color.Gray,
                                    modifier = Modifier.clickable {
                                        toggleLike(p.id, p.likedBy)
                                    }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("${p.likes}")
                            }

                            Icon(
                                Icons.Filled.ChatBubble,
                                null,
                                modifier = Modifier.clickable {
                                    nav.navigate("postComments/${p.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (previewUrl.isNotBlank()) {
        Dialog(onDismissRequest = { previewUrl = "" }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
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
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { saveEditedPost() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
