package com.example.focusticks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun ProfileScreen(nav: NavHostController) {

    val user = Firebase.auth.currentUser ?: run {
        nav.navigate("login") { popUpTo(0) }
        return
    }

    val uid = user.uid
    val db = Firebase.firestore

    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }

    DisposableEffect(uid) {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    name = doc.getString("name") ?: ""
                    studentId = doc.getString("studentId") ?: ""
                    email = doc.getString("email") ?: (user.email ?: "")
                    phone = doc.getString("phone") ?: ""
                } else {
                    db.collection("users").document(uid)
                        .set(
                            mapOf(
                                "email" to email,
                                "name" to "",
                                "studentId" to "",
                                "phone" to ""
                            ),
                            SetOptions.merge()
                        )
                }
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                }
                Row {
                    Text(
                        text = if (!editing) "Edit" else "Save",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 20.dp).clickable {
                            if (editing) {
                                db.collection("users").document(uid)
                                    .set(
                                        mapOf(
                                            "name" to name,
                                            "studentId" to studentId,
                                            "email" to email,
                                            "phone" to phone
                                        ),
                                        SetOptions.merge()
                                    )
                            }
                            editing = !editing
                        }
                    )
                    Text(
                        text = "Logout",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            Firebase.auth.signOut()
                            nav.navigate("login") { popUpTo(0) }
                        }
                    )
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize()
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { if (editing) name = it },
                label = { Text("Name") },
                enabled = editing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { if (editing) studentId = it },
                label = { Text("Student ID") },
                enabled = editing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { if (editing) email = it },
                label = { Text("Email") },
                enabled = editing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { if (editing) phone = it },
                label = { Text("Phone No") },
                enabled = editing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "View Streaks",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { nav.navigate("streak") }
            )
        }
    }
}
