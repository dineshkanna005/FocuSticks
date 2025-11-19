package com.example.focusticks.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun ForgotPasswordScreen(nav: NavHostController) {

    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {

            Text(
                text = "FocuSticks",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(text = "Email:-", color = Color.White, fontSize = 18.sp)

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    message = ""
                },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            )

            if (message.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(message, color = Color.Cyan, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        sending = true
                        Firebase.auth.sendPasswordResetEmail(email.trim())
                            .addOnSuccessListener {
                                sending = false
                                message = "Reset link sent to email."
                            }
                            .addOnFailureListener {
                                sending = false
                                message = "Error. Try again."
                            }
                    }
                },
                enabled = email.isNotBlank() && !sending,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
            ) {
                Text(
                    if (sending) "Sending…" else "Send Reset Link",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Back to Login",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.clickable {
                    nav.navigate("login")
                }
            )
        }
    }
}
