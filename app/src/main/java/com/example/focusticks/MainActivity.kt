package com.example.focusticks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
private sealed class Route(val name: String) {
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object Dashboard : Route("dashboard")
    data object Profile : Route("profile")
    data object Task : Route("task")
    data object Leaderboard : Route("leaderboard")
    data object Discussion : Route("discussion")
}
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
@Composable
private fun AppRoot() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Route.Splash.name
    ) {
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
                onForgot = { /* TODO */ },
                onSignUp = { /* TODO */ }
            )
        }

        composable(Route.Dashboard.name) { DashboardScreen(nav) }
        composable(Route.Profile.name) { ProfileScreen(nav) }
        composable(Route.Task.name) { TaskScreen(nav) }
        composable(Route.Leaderboard.name) { LeaderboardScreen(nav) }
        composable(Route.Discussion.name) { DiscussionScreen(nav) }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBarWithBack(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
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

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FocuSticks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
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
                Text(
                    if (showPw) "Hide" else "Show",
                    modifier = Modifier
                        .clickable { showPw = !showPw }
                        .padding(8.dp)
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
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
private fun DashboardScreen(nav: NavHostController) {
    val entries: List<Triple<String, String, @Composable () -> Unit>> = listOf(
        Triple("Profile", Route.Profile.name) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = null)
        },
        Triple("Task", Route.Task.name) {
            Icon(Icons.Outlined.Build, contentDescription = null)
        },
        Triple("Leaderboard", Route.Leaderboard.name) {
            Icon(Icons.Outlined.Leaderboard, contentDescription = null)
        },
        Triple("Discussion", Route.Discussion.name) {
            Icon(Icons.Outlined.Chat, contentDescription = null)
        }
    )

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                entries.forEachIndexed { index, triple ->
                    val (label, route, leading) = triple
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(route) }
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        leading()
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                    if (index != entries.lastIndex) Divider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(nav: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // TODO: add real sign-out if you wire auth
                            nav.navigate(Route.Login.name) {
                                popUpTo(Route.Dashboard.name) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard {
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.AccountCircle, contentDescription = null) }
            }

            Spacer(Modifier.height(24.dp))
            LabeledValue("Name", "Dinesh Kanna")
            LabeledValue("Student id", "U12345678")
            LabeledValue("Email", "dineshkanna1810@gmail.com")
            LabeledValue("Phone no", "555-123-4567")

            Spacer(Modifier.height(24.dp))
            Button(onClick = { /* TODO: streaks */ }) { Text("Streaks") }
        }
    }
}

@Composable
private fun TaskScreen(nav: NavHostController) {
    Scaffold(topBar = { TopBarWithBack("Task") { nav.popBackStack() } }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp)) {
            Text("Task Screen", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun LeaderboardScreen(nav: NavHostController) {
    Scaffold(topBar = { TopBarWithBack("Leaderboard") { nav.popBackStack() } }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp)) {
            Text("Leaderboard Screen", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun DiscussionScreen(nav: NavHostController) {
    Scaffold(topBar = { TopBarWithBack("Discussion") { nav.popBackStack() } }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp)) {
            Text("Discussion Screen", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
@Composable
private fun LabeledValue(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
        Divider(Modifier.padding(vertical = 8.dp))
    }
}
