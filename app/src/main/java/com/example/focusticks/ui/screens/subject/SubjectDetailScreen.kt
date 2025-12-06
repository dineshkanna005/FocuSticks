package com.example.focusticks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

data class ResourceItem(
    val id: String = "",
    val title: String = "",
    val url: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(nav: NavHostController, subjectId: String) {

    var subjectName by remember { mutableStateOf(subjectId) }
    var resources by remember { mutableStateOf(listOf<ResourceItem>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore

        db.collection("subjects").document(subjectId)
            .get()
            .addOnSuccessListener { doc ->
                subjectName = doc.getString("name") ?: subjectId
            }

        db.collection("subjects").document(subjectId)
            .collection("resources")
            .get()
            .addOnSuccessListener { snap ->
                resources = snap.documents.map { d ->
                    ResourceItem(
                        id = d.id,
                        title = d.getString("title") ?: "",
                        url = d.getString("url") ?: ""
                    )
                }
                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(subjectName, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { pad ->

        if (loading) {
            Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(resources) { res ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp)
                        .clickable {
                            nav.navigate("resource_viewer/$subjectId/${res.id}")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            res.title,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
