package com.example.focusticks.ui.screens.posts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(nav: NavHostController, openDrawer: () -> Unit) {

    val uid = Firebase.auth.currentUser?.uid ?: return
    val db = Firebase.firestore
    val storage = Firebase.storage
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("Student") }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener {
            username = it.getString("name") ?: it.getString("username") ?: "Student"
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text("Create Post", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Menu,
                    null,
                    modifier = Modifier.clickable { openDrawer() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { pad ->

        Column(
            Modifier
                .padding(pad)
                .padding(20.dp)
        ) {

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("Share your study update...") }
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { launcher.launch("image/*") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Image")
            }

            imageUri?.let {
                Spacer(Modifier.height(12.dp))
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            uploading = true
                            var uploadedUrl = ""

                            if (imageUri != null) {
                                val ref = storage.reference.child("posts/${UUID.randomUUID()}")
                                ref.putFile(imageUri!!).await()
                                uploadedUrl = ref.downloadUrl.await().toString()
                            }

                            val post = mapOf(
                                "uid" to uid,
                                "username" to username,
                                "text" to text,
                                "imageUrl" to uploadedUrl,
                                "timestamp" to System.currentTimeMillis(),
                                "likes" to 0,
                                "likedBy" to emptyList<String>()
                            )

                            db.collection("posts").add(post).await()

                            uploading = false
                            nav.popBackStack()

                        } catch (e: Exception) {
                            uploading = false
                        }
                    }
                },
                enabled = text.isNotBlank() && !uploading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Post")
            }
        }
    }
}
