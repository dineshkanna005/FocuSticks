package com.example.focusticks.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    var showButton by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (showButton) 1f else 0f,
        animationSpec = tween(1000),
        label = "fade"
    )

    LaunchedEffect(Unit) {
        delay(1500)
        showButton = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FocuSticks",
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(16.dp))
            if (!showButton) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text(text = "Get Started")
                }
            }
        }
    }
}
