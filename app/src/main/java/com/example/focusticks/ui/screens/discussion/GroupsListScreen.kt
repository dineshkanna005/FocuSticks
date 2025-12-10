package com.example.focusticks.ui.screens.discussion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
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
fun GroupsListScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid ?: ""

    var groups by remember { mutableStateOf(listOf<GroupItem>()) }

    LaunchedEffect(Unit) {
        db.collection("groups")
            .addSnapshotListener { snap, _ ->
                groups = snap?.documents?.mapNotNull {
                    GroupItem(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        code = it.getString("code") ?: "",
                        adminId = it.getString("adminId") ?: "",
                        members = it.get("members") as? List<String> ?: emptyList()
                    )
                }?.filter { it.members.contains(uid) } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                    Text("Discussion", style = MaterialTheme.typography.headlineSmall)
                }
                Row {
                    IconButton(onClick = { nav.navigate("joinGroup") }) { Icon(Icons.Default.Group, null) }
                    IconButton(onClick = { nav.navigate("createGroup") }) { Icon(Icons.Default.Add, null) }
                    IconButton(onClick = { openDrawer() }) { Icon(Icons.Default.Menu, null) }
                }
            }
        }
    ) { pad ->

        LazyColumn(
            modifier = Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(groups) { group ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(group.name, style = MaterialTheme.typography.titleMedium)

                            if (group.adminId == uid) {
                                IconButton(onClick = {
                                    db.collection("groups").document(group.id).delete()
                                }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text("${group.members.size} members")

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = { nav.navigate("groupChat/${group.id}") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Chat")
                        }
                    }
                }
            }
        }
    }
}
