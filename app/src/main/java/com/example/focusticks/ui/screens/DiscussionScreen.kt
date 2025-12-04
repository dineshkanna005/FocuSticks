package com.example.focusticks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.focusticks.DiscussionItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(nav: NavHostController) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""

    var descriptions by remember { mutableStateOf(listOf<DiscussionItem>()) }
    var comments by remember { mutableStateOf(listOf<DiscussionItem>()) }

    var inputDesc by remember { mutableStateOf("") }
    var inputComment by remember { mutableStateOf("") }
    var selectedDescId by remember { mutableStateOf<String?>(null) }
    var replyTo by remember { mutableStateOf<DiscussionItem?>(null) }

    var editDescMode by remember { mutableStateOf(false) }
    var editingDescId by remember { mutableStateOf<String?>(null) }

    var editCommentMode by remember { mutableStateOf(false) }
    var editingCommentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        db.collection("discussion")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                descriptions = snap?.documents?.mapNotNull { d ->
                    d.toObject(DiscussionItem::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
    }

    fun loadComments(id: String) {
        db.collection("discussion")
            .document(id)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                comments = snap?.documents?.mapNotNull { d ->
                    d.toObject(DiscussionItem::class.java)?.copy(id = d.id)
                } ?: emptyList()
            }
    }

    fun saveDescription() {
        if (inputDesc.isBlank()) return

        if (editDescMode && editingDescId != null) {
            db.collection("discussion").document(editingDescId!!)
                .update(mapOf("text" to inputDesc.trim()))
            editDescMode = false
            editingDescId = null
        } else {
            db.collection("users").document(uid).get().addOnSuccessListener { u ->
                val name = u.getString("name") ?: "User"
                db.collection("discussion").add(
                    DiscussionItem(
                        text = inputDesc.trim(),
                        uid = uid,
                        userName = name,
                        timestamp = Timestamp.now()
                    )
                )
            }
        }
        inputDesc = ""
    }

    fun deleteDescription(id: String) {
        db.collection("discussion").document(id).delete()
    }

    fun saveComment() {
        if (inputComment.isBlank() || selectedDescId.isNullOrEmpty()) return

        if (editCommentMode && editingCommentId != null) {
            db.collection("discussion").document(selectedDescId!!)
                .collection("comments").document(editingCommentId!!)
                .update(mapOf("text" to inputComment.trim()))

            editCommentMode = false
            editingCommentId = null
            inputComment = ""
            replyTo = null
            return
        }

        db.collection("users").document(uid).get().addOnSuccessListener { u ->
            val name = u.getString("name") ?: "User"
            val newComment = DiscussionItem(
                text = inputComment.trim(),
                uid = uid,
                userName = name,
                timestamp = Timestamp.now(),
                replyToId = replyTo?.id
            )
            db.collection("discussion").document(selectedDescId!!)
                .collection("comments").add(newComment)
        }
        inputComment = ""
        replyTo = null
    }

    fun deleteComment(commentId: String) {
        if (selectedDescId == null) return
        db.collection("discussion").document(selectedDescId!!)
            .collection("comments").document(commentId).delete()
    }

    @Composable
    fun RenderComment(item: DiscussionItem, depth: Int) {
        val replies = comments.filter { it.replyToId == item.id }

        Column(
            Modifier.padding(start = (depth * 20).dp, top = 8.dp)
        ) {

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(Modifier.padding(12.dp)) {

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.userName, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        if (item.uid == uid) {
                            Row {
                                Text(
                                    "Edit",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        editCommentMode = true
                                        editingCommentId = item.id
                                        inputComment = item.text
                                    }
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Delete",
                                    color = Color.Red,
                                    modifier = Modifier.clickable {
                                        deleteComment(item.id)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(item.text, fontSize = 14.sp)

                    Spacer(Modifier.height(6.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "Reply",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                replyTo = item
                                inputComment = ""
                                editCommentMode = false
                            }
                        )
                    }
                }
            }

            replies.forEach { RenderComment(it, depth + 1) }
        }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.clickable { nav.popBackStack() },
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Discussion",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    ) { pad ->

        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize()
        ) {

            OutlinedTextField(
                value = inputDesc,
                onValueChange = { inputDesc = it },
                label = { Text(if (editDescMode) "Edit Description" else "Add Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { saveDescription() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (editDescMode) "Update" else "Save", fontSize = 16.sp)
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn(Modifier.weight(1f)) {
                items(descriptions) { desc ->

                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {

                        Card(
                            Modifier.fillMaxWidth().clickable {
                                selectedDescId =
                                    if (selectedDescId == desc.id) null else desc.id
                                if (selectedDescId != null) loadComments(desc.id)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEAFF))
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        desc.text,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    if (desc.uid == uid) {
                                        Row {
                                            Text(
                                                "Edit",
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    editDescMode = true
                                                    editingDescId = desc.id
                                                    inputDesc = desc.text
                                                }
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                "Delete",
                                                color = Color.Red,
                                                modifier = Modifier.clickable {
                                                    deleteDescription(desc.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedDescId == desc.id) {

                            comments.filter { it.replyToId == null }.forEach {
                                RenderComment(it, 0)
                            }

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = inputComment,
                                onValueChange = { inputComment = it },
                                label = {
                                    Text(
                                        when {
                                            editCommentMode -> "Edit Comment"
                                            replyTo != null -> "Reply to ${replyTo!!.userName}"
                                            else -> "Add Comment"
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick = { saveComment() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    when {
                                        editCommentMode -> "Update"
                                        else -> "Post"
                                    },
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
