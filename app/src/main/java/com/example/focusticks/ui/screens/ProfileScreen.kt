package com.example.focusticks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavHostController) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val email = Firebase.auth.currentUser?.email ?: ""
    val db = Firebase.firestore

    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                name = snap?.getString("name") ?: ""
                studentId = snap?.getString("studentId") ?: ""
                phone = snap?.getString("phone") ?: ""
            }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Edit",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .clickable { editing = true }
                )
                Text(
                    text = "Logout",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { Firebase.auth.signOut() }
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            if (editing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("Student ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone no") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        db.collection("users").document(uid)
                            .set(
                                mapOf(
                                    "name" to name.trim(),
                                    "studentId" to studentId.trim(),
                                    "phone" to phone.trim(),
                                    "updatedAt" to Timestamp.now()
                                ),
                                SetOptions.merge()
                            )
                        editing = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save")
                }
            } else {
                Text("Name", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = name,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                Text("Student ID", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = studentId,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                Text("Email", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                Text("Phone no", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = phone,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "View streaks",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { nav.navigate("streak") }
                )
            }
        }
    }
}
