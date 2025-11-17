package com.example.focusticks.ui.screens

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            onValueChange = {
                email = it
                if (errorText.isNotEmpty()) errorText = ""
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (errorText.isNotEmpty()) errorText = ""
            },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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

        if (errorText.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = errorText,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }

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

                val auth = Firebase.auth
                loading = true

                auth.signInWithEmailAndPassword(trimmedEmail, password)
                    .addOnSuccessListener {
                        loading = false
                        nav.navigate("dashboard") { launchSingleTop = true }
                    }
                    .addOnFailureListener { ex ->
                        loading = false
                        val code = (ex as? FirebaseAuthException)?.errorCode ?: ""
                        errorText = when (code) {
                            "ERROR_WRONG_PASSWORD",
                            "ERROR_INVALID_CREDENTIAL",
                            "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                                "The password you entered is incorrect."
                            "ERROR_USER_NOT_FOUND" ->
                                "No account found with this email."
                            "ERROR_INVALID_EMAIL" ->
                                "Enter a valid email address."
                            else ->
                                "Login failed. Please try again."
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
