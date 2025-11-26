package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase

@Composable
fun ProfileScreen(nav: NavHostController) {

    val auth = Firebase.auth
    val uid = auth.currentUser?.uid ?: return
    val dbRef = Firebase.firestore.collection("users").document(uid)

    var name by remember { mutableStateOf("") }
    val email by remember { mutableStateOf(auth.currentUser?.email ?: "N/A") }
    var studentId by remember { mutableStateOf("") }
    var phoneNo by remember { mutableStateOf("") }
    var points by remember { mutableStateOf(0L) }
    var lastTaskCompleted by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dbRef.get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject<User>() ?: User(uid = uid)
                name = user.name
                studentId = user.studentId
                phoneNo = user.phoneNo
                points = user.points
                lastTaskCompleted = user.lastTaskCompleted
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun saveProfile() {
        val data = mapOf(
            "name" to name.trim(),
            "studentId" to studentId.trim(),
            "phoneNo" to phoneNo.trim(),
            "points" to points,
            "lastTaskCompleted" to lastTaskCompleted
        )
        dbRef.set(data, SetOptions.merge())
        isEditing = false
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.clickable { nav.popBackStack() }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isEditing) "Save" else "Edit",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable {
                                if (isEditing) saveProfile() else isEditing = true
                            },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Logout",
                        modifier = Modifier.clickable {
                            auth.signOut()
                            nav.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { pad ->

        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(pad)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                readOnly = !isEditing,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Student ID") },
                readOnly = !isEditing,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {},
                label = { Text("Email") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneNo,
                onValueChange = { phoneNo = it },
                label = { Text("Phone No") },
                readOnly = !isEditing,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Text(
                "View Streaks",
                modifier = Modifier.clickable { nav.navigate("streak") },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
