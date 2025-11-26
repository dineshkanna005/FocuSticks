package com.example.focusticks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

data class Topic(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(nav: NavHostController) {

    val db = Firebase.firestore
    var topics by remember { mutableStateOf(listOf<Topic>()) }
    var showNewTopic by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    val uid = Firebase.auth.currentUser?.uid ?: ""

    DisposableEffect(Unit) {
        val listener = db.collection("topics")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                topics = snap?.documents
                    ?.map { d -> d.toObject(Topic::class.java)?.copy(id = d.id) ?: Topic(id = d.id) }
                    ?: emptyList()
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion Topics") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showNewTopic = true }) {
                        Icon(Icons.Filled.Add, null)
                    }
                }
            )
        }
    ) { pad ->

        if (showNewTopic) {
            AlertDialog(
                onDismissRequest = { showNewTopic = false },
                confirmButton = {
                    Button(onClick = {
                        if (newTitle.isNotBlank()) {
                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { u ->
                                    val name = u.getString("name") ?: "User"
                                    db.collection("topics").add(
                                        mapOf(
                                            "title" to newTitle.trim(),
                                            "description" to newDesc.trim(),
                                            "creatorUid" to uid,
                                            "creatorName" to name,
                                            "timestamp" to Timestamp.now()
                                        )
                                    )
                                    newTitle = ""
                                    newDesc = ""
                                    showNewTopic = false
                                }
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    Button(onClick = { showNewTopic = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("New Topic") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Topic Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newDesc,
                            onValueChange = { newDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            items(topics) { topic ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable {
                            nav.navigate("discussion/${topic.id}")
                        }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(topic.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(topic.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
