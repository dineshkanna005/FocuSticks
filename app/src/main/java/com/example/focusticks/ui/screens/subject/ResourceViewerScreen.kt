package com.example.focusticks.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceViewerScreen(nav: NavHostController, subjectId: String, resourceId: String) {

    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("subjects")
            .document(subjectId)
            .collection("resources")
            .document(resourceId)
            .get()
            .addOnSuccessListener { doc ->
                title = doc.getString("title") ?: ""
                url = doc.getString("url") ?: ""
                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
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

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()

                    if (url.lowercase().endsWith(".pdf")) {
                        loadUrl("https://docs.google.com/gview?embedded=true&url=$url")
                    } else {
                        loadUrl(url)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        )
    }
}
