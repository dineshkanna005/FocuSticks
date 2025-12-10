package com.example.focusticks.ui.screens.discussion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""

    var groupName by remember { mutableStateOf("") }
    var groupCode by remember { mutableStateOf("") }
    var showCreated by remember { mutableStateOf(false) }

    fun createGroup() {
        if (groupName.isBlank()) return

        val finalCode = if (groupCode.isBlank()) {
            (100000..999999).random().toString()
        } else groupCode.trim().uppercase()

        val data = mapOf(
            "name" to groupName.trim(),
            "code" to finalCode,
            "adminId" to uid,
            "members" to listOf(uid)
        )

        db.collection("groups")
            .add(data)
            .addOnSuccessListener {
                groupCode = finalCode
                showCreated = true
            }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                    Text("Create Group", style = MaterialTheme.typography.headlineSmall)
                }

                IconButton(onClick = { openDrawer() }) {
                    Icon(Icons.Default.Menu, null)
                }
            }
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = groupCode,
                onValueChange = { groupCode = it },
                label = { Text("Group Code (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { createGroup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Create Group")
            }

            if (showCreated) {
                Spacer(Modifier.height(30.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Group Created!", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(14.dp))
                        Text("Code:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            groupCode.uppercase(),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { nav.popBackStack() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
