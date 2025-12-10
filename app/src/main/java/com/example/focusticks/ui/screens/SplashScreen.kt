package com.example.focusticks.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: (String) -> Unit) {

    var start by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    val alpha = animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(1200)
    )

    val scale = animateFloatAsState(
        targetValue = if (start) 1f else 0.7f,
        animationSpec = tween(1200)
    )

    LaunchedEffect(Unit) {
        start = true
        delay(1400)
        showButton = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "FocuSticks",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .alpha(alpha.value)
                    .scale(scale.value)
            )

            Spacer(Modifier.height(60.dp))

            if (showButton) {
                Button(
                    onClick = {
                        val user = Firebase.auth.currentUser
                        if (user != null) onDone("dashboard") else onDone("login")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
