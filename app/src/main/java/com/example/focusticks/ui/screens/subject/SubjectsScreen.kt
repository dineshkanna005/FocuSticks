package com.example.focusticks.ui.screens.subject

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class SubjectItem(
    val id: String = "",
    val name: String = "",
    val semester: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(nav: NavHostController, openDrawer: () -> Unit) {

    var subjects by remember { mutableStateOf(listOf<SubjectItem>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("subjects")
            .get()
            .addOnSuccessListener { snap ->
                subjects = snap.documents.map { doc ->
                    SubjectItem(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id,
                        semester = doc.getString("semester") ?: ""
                    )
                }
                loading = false
            }
            .addOnFailureListener { loading = false }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "",
                        modifier = Modifier.clickable { nav.popBackStack() },
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("Subjects", style = MaterialTheme.typography.headlineMedium)
                }
                Icon(
                    Icons.Filled.Menu,
                    "",
                    modifier = Modifier.clickable { openDrawer() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
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
            items(subjects) { subject ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .clickable { nav.navigate("subject_detail/${subject.id}") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(subject.name, fontSize = 20.sp)
                            if (subject.semester.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    subject.semester,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
