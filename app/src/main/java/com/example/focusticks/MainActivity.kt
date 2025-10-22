package com.example.focusticks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.focusticks.ui.theme.FocuSticksTheme
import kotlinx.coroutines.launch

private sealed class Route(val name: String) {
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object Signup : Route("signup")
    data object ForgotStep1 : Route("forgot_step_1")
    data object ForgotStep2 : Route("forgot_step_2")
    data object Dashboard : Route("dashboard")
    data object Discussion : Route("discussion")
    data object Leaderboard : Route("leaderboard")
    data object Task : Route("task")
    data object Profile : Route("profile")
}

private data class AuthState(
    val isLoggedIn: Boolean = false,
    val email: String? = null
)

@Stable
private class AuthController(initial: AuthState) {
    var state by mutableStateOf(initial)
        private set
    fun login(email: String): Boolean {
        val ok = email.trim().equals("dineshkanna1810@gmail.com", true) ||
                email.trim().equals("din1810@gmail.com", true)
        if (ok) state = AuthState(true, email.trim())
        return ok
    }
    fun signup(email: String) {
        state = AuthState(true, email.trim())
    }
    fun logout() {
        state = AuthState(isLoggedIn = false)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocuSticksTheme {
                val auth = remember { AuthController(AuthState()) }
                AppRoot(auth)
            }
        }
    }
}

@Composable
private fun AppRoot(auth: AuthController) {
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route
    Scaffold(
        bottomBar = { if (shouldShowBottomBar(currentRoute)) BottomBar(nav, currentRoute) }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Route.Splash.name,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Splash.name) { SplashScreen(nav, auth) }
            composable(Route.Login.name) { LoginScreen(nav, auth) }
            composable(Route.Signup.name) { SignupScreen(nav, auth) }
            composable(Route.ForgotStep1.name) { ForgotStep1Screen(nav) }
            composable(Route.ForgotStep2.name) { ForgotStep2Screen(nav) }
            composable(Route.Dashboard.name) { DashboardScreen(nav) }
            composable(Route.Discussion.name) { DiscussionScreen(nav) }
            composable(Route.Leaderboard.name) { LeaderboardScreen(nav) }
            composable(Route.Task.name) { TaskScreen(nav) }
            composable(Route.Profile.name) { ProfileScreen(nav, auth) }
        }
    }
}

private fun shouldShowBottomBar(route: String?): Boolean =
    route in setOf(
        Route.Dashboard.name,
        Route.Discussion.name,
        Route.Leaderboard.name,
        Route.Task.name,
        Route.Profile.name
    )

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

private val bottomItems = listOf(
    BottomItem(Route.Discussion.name, "Discussion") { Icon(Icons.Outlined.Chat, null) },
    BottomItem(Route.Leaderboard.name, "Leaderboard") { Icon(Icons.Outlined.Leaderboard, null) },
    BottomItem(Route.Task.name, "Task") { Icon(Icons.Outlined.Build, null) },
    BottomItem(Route.Profile.name, "Profile") { Icon(Icons.Outlined.AccountCircle, null) }
)

@Composable
private fun BottomBar(nav: NavHostController, currentRoute: String?) {
    NavigationBar {
        bottomItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    nav.navigate(item.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = item.icon,
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
private fun SimpleTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Row(content = actions)
        }
    }
}

@Composable
private fun SplashScreen(nav: NavHostController, auth: AuthController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FocuSticks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (auth.state.isLoggedIn) {
                    nav.navigate(Route.Dashboard.name) {
                        popUpTo(Route.Splash.name) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    nav.navigate(Route.Login.name) {
                        popUpTo(Route.Splash.name) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        ) { Text("Get Started") }
    }
}

@Composable
private fun LoginScreen(nav: NavHostController, auth: AuthController) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FocuSticks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email:-") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password:-") },
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
                    "Forget Password",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { nav.navigate(Route.ForgotStep1.name) }
                )
                Text(
                    "Sign up",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { nav.navigate(Route.Signup.name) }
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val ok = email.isNotBlank() && password.isNotBlank() && auth.login(email)
                    if (ok) {
                        nav.navigate(Route.Dashboard.name) {
                            popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Invalid email or password") }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirm") }
        }
    }
}

@Composable
private fun SignupScreen(nav: NavHostController, auth: AuthController) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var email by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var show1 by rememberSaveable { mutableStateOf(false) }
    var show2 by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = { SimpleTopBar(title = "Sign up", onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Mail id") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = dob, onValueChange = { dob = it.take(10) },
                label = { Text("Date of birth") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Create Password") }, singleLine = true,
                visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { show1 = !show1 }) { Text(if (show1) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it },
                label = { Text("Confirm Password") }, singleLine = true,
                visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { show2 = !show2 }) { Text(if (show2) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    when {
                        email.isBlank() || dob.isBlank() || pass.isBlank() || confirm.isBlank() ->
                            scope.launch { snackbarHostState.showSnackbar("Fill all fields") }
                        pass != confirm ->
                            scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                        else -> {
                            auth.signup(email)
                            nav.navigate(Route.Dashboard.name) {
                                popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirm") }
        }
    }
}

@Composable
private fun ForgotStep1Screen(nav: NavHostController) {
    var email by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { SimpleTopBar(title = "Forgot Password", onBack = { nav.popBackStack() }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Mail id") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = dob, onValueChange = { dob = it.take(10) },
                label = { Text("Date of Birth MM/DD/YYYY") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { nav.navigate(Route.ForgotStep2.name) }, modifier = Modifier.fillMaxWidth()) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun ForgotStep2Screen(nav: NavHostController) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var p1 by rememberSaveable { mutableStateOf("") }
    var p2 by rememberSaveable { mutableStateOf("") }
    var show1 by rememberSaveable { mutableStateOf(false) }
    var show2 by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = { SimpleTopBar(title = "Reset Password", onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = p1, onValueChange = { p1 = it },
                label = { Text("Create New Password") }, singleLine = true,
                visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { show1 = !show1 }) { Text(if (show1) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = p2, onValueChange = { p2 = it },
                label = { Text("Confirm Password") }, singleLine = true,
                visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { show2 = !show2 }) { Text(if (show2) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (p1.isBlank() || p2.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Enter both fields") }
                    } else if (p1 != p2) {
                        scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Password reset successful") }
                        nav.navigate(Route.Login.name) {
                            popUpTo(Route.Login.name) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirm") }
        }
    }
}

@Composable
private fun DashboardScreen(nav: NavHostController) {
    val entries = listOf(
        Triple("Profile", Route.Profile.name, Icons.Outlined.AccountCircle),
        Triple("Task", Route.Task.name, Icons.Outlined.Build),
        Triple("Leaderboard", Route.Leaderboard.name, Icons.Outlined.Leaderboard),
        Triple("Discussion", Route.Discussion.name, Icons.Outlined.Chat)
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                entries.forEachIndexed { index, (label, route, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(route) }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                    if (index != entries.lastIndex) Divider()
                }
            }
        }
    }
}

@Composable
private fun TaskScreen(nav: NavHostController) {
    Scaffold(topBar = { SimpleTopBar(title = "Task", onBack = { nav.popBackStack() }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize()
        ) {
            SectionTaskItem(title = "Assignment")
            Spacer(Modifier.height(12.dp))
            SectionTaskItem(title = "Quiz")
            Spacer(Modifier.height(12.dp))
            SectionTaskItem(title = "Project")
        }
    }
}

@Composable
private fun SectionTaskItem(title: String) {
    var level by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf("") }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = level, onValueChange = { level = it }, label = { Text("Level:-") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = due, onValueChange = { due = it.take(10) }, label = { Text("Due Date: mm/dd/yy") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LeaderboardScreen(nav: NavHostController) {
    val sample = listOf("Aarav" to 100, "Taylor" to 95, "Michael" to 94)
    Scaffold(topBar = { SimpleTopBar(title = "Leaderboard", onBack = { nav.popBackStack() }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize()
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Name", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("Points", style = MaterialTheme.typography.titleMedium)
            }
            Divider()
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(sample) { index, (name, points) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}. $name", modifier = Modifier.weight(1f))
                        Text(points.toString())
                    }
                    Divider()
                }
            }
            TextButton(onClick = {}, modifier = Modifier.align(Alignment.Start)) { Text("View my position") }
        }
    }
}

@Composable
private fun DiscussionScreen(nav: NavHostController) {
    Scaffold(topBar = { SimpleTopBar(title = "Discussion", onBack = { nav.popBackStack() }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Discussion – coming soon")
        }
    }
}

@Composable
private fun ProfileScreen(nav: NavHostController, auth: AuthController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showConfirm by remember { mutableStateOf(false) }
    val email = auth.state.email ?: "dineshkanna1810@gmail.com"
    val name = remember(email) {
        email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    val studentId = "00947890"
    val phone = "+1 (203)0109999"
    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "Profile",
                onBack = { nav.popBackStack() },
                actions = {
                    TextButton(onClick = { showConfirm = true }) { Text("Logout") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            AvatarLarge()
            Spacer(Modifier.height(20.dp))
            ProfileField(label = "Name", value = name)
            Spacer(Modifier.height(10.dp))
            ProfileField(label = "Student id", value = studentId)
            Spacer(Modifier.height(10.dp))
            ProfileField(label = "Email", value = email)
            Spacer(Modifier.height(10.dp))
            ProfileField(label = "Phone no", value = phone)
            Spacer(Modifier.height(22.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { scope.launch { snackbarHostState.showSnackbar("Streaks — coming soon") } },
                modifier = Modifier.fillMaxWidth(0.5f).height(44.dp)
            ) { Text("Streaks") }
        }
        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirm = false
                            auth.logout()
                            nav.navigate(Route.Login.name) {
                                popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                            scope.launch { snackbarHostState.showSnackbar("Logged out") }
                        }
                    ) { Text("Yes, Logout") }
                },
                dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors()
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AvatarLarge() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        modifier = Modifier.size(96.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Avatar",
                modifier = Modifier.size(72.dp)
            )
        }
    }
}
