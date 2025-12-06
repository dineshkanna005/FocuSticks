package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.focusticks.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val auth = Firebase.auth
    val uid = auth.currentUser?.uid ?: return
    val dbRef = Firebase.firestore.collection("users").document(uid)

    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var phoneNo by remember { mutableStateOf("") }
    val email by remember { mutableStateOf(auth.currentUser?.email ?: "N/A") }
    var points by remember { mutableStateOf(0L) }
    var lastTaskCompleted by remember { mutableStateOf(0L) }

    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dbRef.get().addOnSuccessListener {
            val u = it.toObject<User>() ?: User(uid = uid)
            name = u.name
            studentId = u.studentId
            phoneNo = u.phoneNo
            points = u.points
            lastTaskCompleted = u.lastTaskCompleted
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    fun saveProfile() {
        dbRef.set(
            mapOf(
                "name" to name.trim(),
                "studentId" to studentId.trim(),
                "phoneNo" to phoneNo.trim(),
                "points" to points,
                "lastTaskCompleted" to lastTaskCompleted
            ),
            SetOptions.merge()
        )
        isEditing = false
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text("Profile", style = MaterialTheme.typography.headlineSmall)
                }
                IconButton(onClick = { openDrawer() }) {
                    Icon(Icons.Filled.Menu, null)
                }
            }
        }
    ) { pad ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val initials = if (name.isNotBlank()) {
                name.trim().split(" ").map { it.take(1) }.joinToString("").uppercase()
            } else "U"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        if (isEditing) "Save" else "Edit",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                if (isEditing) saveProfile() else isEditing = true
                            }
                            .padding(6.dp),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        name.ifBlank { "Your Name" },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(20.dp)) {

                    Text("Personal Information", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

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
                        value = phoneNo,
                        onValueChange = { phoneNo = it },
                        label = { Text("Phone Number") },
                        readOnly = !isEditing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { nav.navigate("streak") },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(180.dp)
                    .height(48.dp)
            ) {
                Text("View Streaks", color = Color.White)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
