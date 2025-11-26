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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import java.util.UUID

data class DescriptionItem(
    val id: String = "",
    val text: String = "",
    val uid: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(nav: NavHostController) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""
    var descriptions by remember { mutableStateOf(listOf<DescriptionItem>()) }
    var newDescription by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<DescriptionItem?>(null) }

    LaunchedEffect(Unit) {
        db.collection("discussion")
            .addSnapshotListener { snap, _ ->
                descriptions = snap?.documents?.map { d ->
                    DescriptionItem(
                        id = d.id,
                        text = d.getString("description") ?: "",
                        uid = d.getString("uid") ?: ""
                    )
                } ?: emptyList()
            }
    }

    fun saveDescription() {
        if (newDescription.isBlank()) return
        val id = UUID.randomUUID().toString()
        db.collection("discussion").document(id)
            .set(
                mapOf(
                    "description" to newDescription.trim(),
                    "uid" to uid,
                    "timestamp" to Timestamp.now()
                ),
                SetOptions.merge()
            )
        newDescription = ""
    }

    fun updateDescription() {
        val target = editTarget ?: return
        db.collection("discussion").document(target.id)
            .set(
                mapOf(
                    "description" to newDescription.trim(),
                    "uid" to target.uid
                ),
                SetOptions.merge()
            )
        newDescription = ""
        editTarget = null
    }

    fun deleteDescription(id: String) {
        db.collection("discussion").document(id).delete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFEDE7F6)
                )
            )
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {

            Column(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newDescription,
                    onValueChange = { newDescription = it },
                    placeholder = { Text("Add Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (editTarget == null) saveDescription() else updateDescription()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(if (editTarget == null) "Save" else "Update")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                items(descriptions) { item ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(item.text)

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clickable {
                                            newDescription = item.text
                                            editTarget = item
                                        }
                                )
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clickable {
                                            deleteDescription(item.id)
                                        }
                                )
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    Icons.Filled.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clickable {
                                            nav.navigate("comments/${item.id}")
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
