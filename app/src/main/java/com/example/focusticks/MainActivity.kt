package com.example.focusticks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.focusticks.ui.theme.FocuSticksTheme
import androidx.compose.foundation.text.KeyboardOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocuSticksTheme {
                AppRoot()
            }
        }
    }
}

private sealed class Route(val name: String) {
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object SignUp : Route("signup")
    data object ForgotStep1 : Route("forgot_step1")
    data object ForgotStep2 : Route("forgot_step2")
    data object Dashboard : Route("dashboard")

    // Dashboard destinations
    data object Profile : Route("profile")
    data object Task : Route("task")
    data object Leaderboard : Route("leaderboard")
    data object Discussion : Route("discussion")
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Route.Splash.name) {

        composable(Route.Splash.name) {
            SplashScreen(onGetStarted = {
                nav.navigate(Route.Login.name) {
                    popUpTo(Route.Splash.name) { inclusive = true }
                }
            })
        }

        composable(Route.Login.name) {
            LoginScreen(
                onConfirm = {
                    nav.navigate(Route.Dashboard.name) {
                        popUpTo(Route.Login.name) { inclusive = true }
                    }
                },
                onForgot = { nav.navigate(Route.ForgotStep1.name) },
                onSignUp = { nav.navigate(Route.SignUp.name) }
            )
        }

        composable(Route.SignUp.name) {
            SignUpScreen(
                onConfirm = {
                    nav.navigate(Route.Dashboard.name) {
                        popUpTo(Route.SignUp.name) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.ForgotStep1.name) {
            ForgotPasswordStep1Screen(onNext = { nav.navigate(Route.ForgotStep2.name) })
        }

        composable(Route.ForgotStep2.name) {
            ForgotPasswordStep2Screen(
                onConfirm = {
                    nav.navigate(Route.Login.name) {
                        popUpTo(Route.Login.name) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard + subpages
        composable(Route.Dashboard.name) { DashboardScreen(nav) }
        composable(Route.Profile.name) { ProfileScreen(onLogout = {
            nav.navigate(Route.Login.name) {
                popUpTo(Route.Dashboard.name) { inclusive = true }
            }
        }) }
        composable(Route.Task.name) { TaskScreen() }
        composable(Route.Leaderboard.name) { LeaderboardScreen() }
        composable(Route.Discussion.name) { DiscussionScreen() }
    }
}

@Composable
private fun SplashScreen(onGetStarted: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "FocuSticks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onGetStarted) { Text("Get Started") }
        }
    }
}

@Composable
private fun LoginScreen(
    onConfirm: () -> Unit,
    onForgot: () -> Unit,
    onSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("FocuSticks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw = !showPw }) {
                    Icon(
                        imageVector = if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "Forgot Password",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onForgot() }
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Confirm") }
        Spacer(Modifier.height(8.dp))
        Text(
            "Sign Up",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onSignUp() }
        )
    }
}

@Composable
private fun SignUpScreen(onConfirm: () -> Unit) {
    var mailId by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var createPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var showPw1 by remember { mutableStateOf(false) }
    var showPw2 by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("FocuSticks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = mailId, onValueChange = { mailId = it },
            label = { Text("Mail id") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = dob, onValueChange = { dob = it.take(10) },
            label = { Text("Date of Birth (MM/DD/YYYY)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = createPw, onValueChange = { createPw = it },
            label = { Text("Create Password") }, singleLine = true,
            visualTransformation = if (showPw1) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw1 = !showPw1 }) {
                    Icon(if (showPw1) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPw, onValueChange = { confirmPw = it },
            label = { Text("Confirm Password") }, singleLine = true,
            visualTransformation = if (showPw2) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPw2 = !showPw2 }) {
                    Icon(if (showPw2) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Confirm") }
    }
}

@Composable
private fun ForgotPasswordStep1Screen(onNext: () -> Unit) {
    var mailId by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") } // MM/DD/YYYY

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("FocuSticks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = mailId, onValueChange = { mailId = it },
            label = { Text("Mail id") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = dob, onValueChange = { dob = it.take(10) },
            label = { Text("Date of Birth (MM/DD/YYYY)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
    }
}

@Composable
private fun ForgotPasswordStep2Screen(onConfirm: () -> Unit) {
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var show1 by remember { mutableStateOf(false) }
    var show2 by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("FocuSticks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = newPw, onValueChange = { newPw = it },
            label = { Text("Create New Password") }, singleLine = true,
            visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { show1 = !show1 }) {
                    Icon(if (show1) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPw, onValueChange = { confirmPw = it },
            label = { Text("Confirm Password") }, singleLine = true,
            visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { show2 = !show2 }) {
                    Icon(if (show2) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Confirm") }
    }
}

@Composable
private fun DashboardScreen(nav: NavHostController) {
    val items = listOf(
        "Profile" to Route.Profile.name,
        "Task" to Route.Task.name,
        "Leaderboard" to Route.Leaderboard.name,
        "Discussion" to Route.Discussion.name
    )

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                items.forEachIndexed { i, (label, dest) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(dest) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                    if (i != items.lastIndex) Divider()
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Text("Name: Dinesh Kanna")
        Text("Email: dineshkanna1810@gmail.com")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
    }
}

@Composable
private fun TaskScreen() {
    var level by remember { mutableStateOf(1) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Task", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Level: $level", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { level++ }) { Text("Increment Level (demo)") }
    }
}

@Composable
private fun LeaderboardScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Coming soon…")
    }
}

@Composable
private fun DiscussionScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Discussion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Coming soon…")
    }
}
