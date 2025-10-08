package com.example.focusticks.auth

import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Login", "Sign Up")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome to FocuSticks", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Stay focused, build streaks, climb the leaderboard.")

        Spacer(Modifier.height(16.dp))
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(label) })
            }
        }
        Spacer(Modifier.height(16.dp))
        if (selectedTab == 0) LoginTab(onLoginSuccess) else SignUpTab(onLoginSuccess)
    }
}
@Composable
private fun LoginTab(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val pwValid = password.length >= 6
    val canLogin = emailValid && pwValid

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(if (showPw) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (canLogin) onLoginSuccess() }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Button(onClick = { onLoginSuccess() }, enabled = canLogin, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Login")
        }
        Spacer(Modifier.height(8.dp))
        Text("Forgot password?", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpTab(onAccountCreated: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var dobMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var showPw2 by remember { mutableStateOf(false) }

    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val pwValid = password.length >= 6
    val pwMatch = password == confirm
    val canCreate = emailValid && dobMillis != null && pwValid && pwMatch

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = dobMillis.formatAsDate() ?: "",
            onValueChange = {}, label = { Text("Date of Birth") },
            singleLine = true, readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
            },
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
        )
        if (showDatePicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = dobMillis ?: System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { dobMillis = state.selectedDateMillis; showDatePicker = false }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
            ) { DatePicker(state = state) }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Create Password (min 6)") }, singleLine = true,
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(if (showPw) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it },
            label = { Text("Confirm Password") }, singleLine = true,
            isError = confirm.isNotEmpty() && !pwMatch,
            supportingText = { if (confirm.isNotEmpty() && !pwMatch) Text("Passwords do not match") },
            visualTransformation = if (showPw2) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw2 = !showPw2 }) {
                    Icon(if (showPw2) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onAccountCreated() }, enabled = canCreate, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Create Account")
        }
    }
}

private fun Long?.formatAsDate(): String? {
    this ?: return null
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(this))
}
