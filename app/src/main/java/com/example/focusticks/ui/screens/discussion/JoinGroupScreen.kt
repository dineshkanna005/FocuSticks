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
fun JoinGroupScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""

    var code by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    var joinedGroupName by remember { mutableStateOf("") }

    fun joinGroup() {
        if (code.length < 4) {
            errorText = "Invalid code"
            return
        }

        db.collection("groups")
            .whereEqualTo("code", code.trim().uppercase())
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    errorText = "No group found"
                    return@addOnSuccessListener
                }

                val doc = snap.documents.first()
                val groupId = doc.id
                val name = doc.getString("name") ?: ""

                db.collection("groups").document(groupId)
                    .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
                    .addOnSuccessListener {
                        success = true
                        joinedGroupName = name
                        errorText = ""
                    }
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
                    Text("Join Group", style = MaterialTheme.typography.headlineSmall)
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
                value = code,
                onValueChange = { code = it },
                label = { Text("Enter 6-digit Code") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorText.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(errorText, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { joinGroup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Join Group")
            }

            if (success) {
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
                        Text(
                            "You Joined:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            joinedGroupName,
                            style = MaterialTheme.typography.headlineSmall
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
