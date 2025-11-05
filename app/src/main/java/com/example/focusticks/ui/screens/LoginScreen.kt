package com.example.focusticks.ui.screens

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun LoginScreen(nav: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FocuSticks", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; if (errorText.isNotEmpty()) errorText = "" },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorText.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(text = errorText, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
        } else {
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; if (errorText.isNotEmpty()) errorText = "" },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Forgot Password",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { nav.navigate("forgot_step_1") }
            )
            Text(
                "Sign up",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { nav.navigate("signup") }
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                errorText = ""
                val trimmedEmail = email.trim().lowercase()
                if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                    errorText = "Enter a valid email address."
                    return@Button
                }
                if (password.isBlank()) {
                    errorText = "Enter your password."
                    return@Button
                }

                loading = true
                val auth = Firebase.auth

                auth.signInWithEmailAndPassword(trimmedEmail, password)
                    .addOnSuccessListener {
                        loading = false
                        nav.navigate("dashboard") { launchSingleTop = true }
                    }
                    .addOnFailureListener { ex ->
                        val code = (ex as? FirebaseAuthException)?.errorCode?.uppercase().orEmpty()

                        when {
                            code == "ERROR_WRONG_PASSWORD" -> {
                                loading = false
                                errorText = "The password you entered is incorrect."
                            }
                            code == "ERROR_USER_NOT_FOUND" -> {
                                loading = false
                                errorText = "No account found with this email."
                            }
                            code == "ERROR_INVALID_EMAIL" -> {
                                loading = false
                                errorText = "Enter a valid email address."
                            }
                            else -> {
                                // Ambiguous case like INVALID_LOGIN_CREDENTIALS → check if the email exists
                                auth.fetchSignInMethodsForEmail(trimmedEmail)
                                    .addOnSuccessListener { result ->
                                        loading = false
                                        val methods = result.signInMethods ?: emptyList()
                                        errorText = if (methods.isEmpty()) {
                                            "No account found with this email."
                                        } else {
                                            "The password you entered is incorrect."
                                        }
                                    }
                                    .addOnFailureListener {
                                        loading = false
                                        errorText = "Login failed. Please try again."
                                    }
                            }
                        }
                    }
            },
            enabled = email.isNotBlank() && password.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Signing in…" else "Confirm")
        }
    }
}
