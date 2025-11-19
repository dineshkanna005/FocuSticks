package com.example.focusticks.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun SignupScreen(nav: NavHostController) {

    var step by remember { mutableStateOf(1) }

    var email by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    var newPass by remember { mutableStateOf("") }
    var newConfirmPass by remember { mutableStateOf("") }

    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(40.dp))

        Text("FocuSticks", fontSize = 32.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(40.dp))

        if (step == 1) {

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Mail id") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of birth") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Create Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPass,
                onValueChange = { confirmPass = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = { step = 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Confirm")
            }
        }

        if (step == 2) {

            OutlinedTextField(
                value = email,
                onValueChange = {},
                readOnly = true,
                label = { Text("Mail id") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of Birth MM/DD/YYYY") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = { step = 3 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Next")
            }
        }

        if (step == 3) {

            OutlinedTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = { Text("Create New Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = newConfirmPass,
                onValueChange = { newConfirmPass = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    if (newPass == newConfirmPass && newPass.length >= 6) {
                        Firebase.auth.createUserWithEmailAndPassword(email, newPass)
                            .addOnSuccessListener {
                                nav.navigate("dashboard") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            }
                            .addOnFailureListener { error = it.message ?: "Error" }
                    } else {
                        error = "Passwords don't match"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Confirm")
            }
        }

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}
