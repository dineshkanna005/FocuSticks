package com.example.focusticks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.focusticks.ui.screens.LoginScreen
import com.example.focusticks.ui.theme.FocuSticksTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

private object DB {
    val auth = Firebase.auth
    val fs = Firebase.firestore
}

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
    data object Streaks : Route("streaks")
}

private fun parseDueMillis(dueStr: String): Long? {
    val patterns = listOf("MM/dd/yyyy HH:mm", "MM/dd/yyyy hh:mm a")
    for (p in patterns) {
        try {
            val sdf = SimpleDateFormat(p, Locale.US)
            sdf.isLenient = false
            return sdf.parse(dueStr)?.time
        } catch (_: ParseException) {}
    }
    return null
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        requestNotifPermissionIfNeeded()
        createTaskChannel()
        setContent { FocuSticksTheme { AppRoot() } }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun createTaskChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel("tasks", "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }
    }

    @Composable
    private fun AppRoot() {
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
                composable(Route.Splash.name) { SplashScreen(nav) }
                composable(Route.Login.name) { LoginScreen(nav) }
                composable(Route.Signup.name) { SignupScreen(nav) }
                composable(Route.ForgotStep1.name) { ForgotStep1Screen(nav) }
                composable(Route.ForgotStep2.name) { ForgotStep2Screen(nav) }
                composable(Route.Dashboard.name) { DashboardScreen(nav) }
                composable(Route.Discussion.name) { DiscussionScreen(nav) }
                composable(Route.Leaderboard.name) { LeaderboardScreen(nav) }
                composable(Route.Task.name) { TaskScreen(nav) }
                composable(Route.Profile.name) { ProfileScreen(nav) }
                composable(Route.Streaks.name) { StreaksScreen(nav) }
            }
        }
    }

    private fun shouldShowBottomBar(route: String?): Boolean =
        route in setOf(Route.Dashboard.name, Route.Discussion.name, Route.Leaderboard.name, Route.Task.name, Route.Profile.name)

    private data class BottomItem(val route: String, val label: String, val icon: @Composable () -> Unit)

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
    private fun SimpleTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Row(content = actions)
            }
        }
    }

    @Composable
    private fun SplashScreen(nav: NavHostController) {
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
                    val user = DB.auth.currentUser
                    if (user != null) {
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
    private fun SignupScreen(nav: NavHostController) {
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
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Mail id") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = dob, onValueChange = { dob = it.take(10) }, label = { Text("Date of birth") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Create Password") },
                    singleLine = true,
                    visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { show1 = !show1 }) { Text(if (show1) "Hide" else "Show") } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { TextButton(onClick = { show2 = !show2 }) { Text(if (show2) "Hide" else "Show") } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        when {
                            email.isBlank() || pass.isBlank() || confirm.isBlank() -> scope.launch { snackbarHostState.showSnackbar("Fill all required fields") }
                            pass != confirm -> scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                            else -> {
                                DB.auth.createUserWithEmailAndPassword(email.trim(), pass)
                                    .addOnSuccessListener {
                                        nav.navigate(Route.Dashboard.name) {
                                            popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        scope.launch { snackbarHostState.showSnackbar(e.message ?: "Signup failed") }
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
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Mail id") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = dob, onValueChange = { dob = it.take(10) }, label = { Text("Date of Birth MM/DD/YYYY") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Button(onClick = { nav.navigate(Route.ForgotStep2.name) }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
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
        Scaffold(topBar = { SimpleTopBar(title = "Reset Password", onBack = { nav.popBackStack() }) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                OutlinedTextField(value = p1, onValueChange = { p1 = it }, label = { Text("Create New Password") }, singleLine = true, visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = { show1 = !show1 }) { Text(if (show1) "Hide" else "Show") } }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = p2, onValueChange = { p2 = it }, label = { Text("Confirm Password") }, singleLine = true, visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = { show2 = !show2 }) { Text(if (show2) "Hide" else "Show") } }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (p1.isBlank() || p2.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("Enter both fields") }
                        } else if (p1 != p2) {
                            scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Password reset simulated") }
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
        val ctx = LocalContext.current
        val scope = rememberCoroutineScope()
        val snack = remember { SnackbarHostState() }
        var adding by rememberSaveable { mutableStateOf(false) }
        var title by rememberSaveable { mutableStateOf("") }
        var due by rememberSaveable { mutableStateOf("") }
        var tasks by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
        var showNotifWarning by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            val reg = DB.fs.collection("tasks").orderBy("createdAt")
                .addSnapshotListener { snap, _ ->
                    tasks = snap?.documents?.map {
                        Triple(it.id, it.getString("title").orEmpty(), it.getString("due").orEmpty())
                    }.orEmpty()
                }
            showNotifWarning = !NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            onDispose { reg.remove() }
        }

        Scaffold(
            topBar = { SimpleTopBar(title = "Task", onBack = { nav.popBackStack() }) },
            snackbarHost = { SnackbarHost(snack) }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize()
            ) {
                if (showNotifWarning) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Notifications are disabled", color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = { openAppNotifSettings(ctx) }) { Text("Enable") }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tasks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { adding = !adding }) { Text(if (adding) "Cancel" else "Add") }
                        TextButton(onClick = {
                            scheduleTestReminder(ctx)
                            scope.launch { snack.showSnackbar("Test notification in 5 seconds") }
                        }) { Text("Test notify") }
                        TextButton(onClick = { showNotificationNow(ctx, "Ping now test") }) { Text("Ping now") }
                    }
                }

                if (adding) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = due,
                        onValueChange = { due = it },
                        label = { Text("Due (MM/dd/yyyy HH:mm or MM/dd/yyyy hh:mm AM/PM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val trimmedTitle = title.trim()
                            val trimmedDue = due.trim()
                            val dueMillis = if (trimmedDue.isBlank()) null else parseDueMillis(trimmedDue)
                            if (trimmedTitle.isBlank()) {
                                scope.launch { snack.showSnackbar("Please enter a title") }
                                return@Button
                            }
                            if (trimmedDue.isNotBlank() && dueMillis == null) {
                                scope.launch { snack.showSnackbar("Invalid due format") }
                                return@Button
                            }
                            val data = mapOf(
                                "title" to trimmedTitle,
                                "due" to trimmedDue,
                                "createdAt" to Timestamp.now(),
                                "uid" to (DB.auth.currentUser?.uid ?: "anon")
                            )
                            DB.fs.collection("tasks").add(data).addOnSuccessListener { ref ->
                                scheduleReminder(ctx, ref.id, trimmedTitle, trimmedDue)
                                scope.launch { snack.showSnackbar("Task saved") }
                            }.addOnFailureListener { e ->
                                scope.launch { snack.showSnackbar(e.message ?: "Failed to save task") }
                            }
                            title = ""
                            due = ""
                            adding = false
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.align(Alignment.Start)
                    ) { Text("Save") }
                    Spacer(Modifier.height(12.dp))
                    Divider()
                }

                LazyColumn(Modifier.fillMaxSize()) {
                    if (tasks.isEmpty()) {
                        item {
                            Text(
                                "No tasks yet. Tap “Add” to create one.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        itemsIndexed(tasks, key = { _, t -> t.first }) { i, triple ->
                            val (id, t, d) = triple
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.outlinedCardColors()
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${i + 1}. $t", style = MaterialTheme.typography.titleMedium)
                                        if (d.isNotBlank()) Text("Due: $d", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(
                                        onClick = {
                                            WorkManager.getInstance(ctx).cancelUniqueWork("task_$id")
                                            DB.fs.collection("tasks").document(id).delete()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleReminder(context: Context, workId: String, title: String, dueStr: String) {
        val dueMillis = if (dueStr.isBlank()) null else parseDueMillis(dueStr)
        if (dueStr.isNotBlank() && dueMillis == null) {
            Log.w("TASKS", "Invalid due format; skipping schedule. due='$dueStr'")
            return
        }
        val now = System.currentTimeMillis()
        val target = dueMillis ?: (now + 30_000L)
        var delay = target - now
        if (delay < 5_000L) delay = 5_000L
        val permitted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!permitted) {
            Log.w("TASKS", "POST_NOTIFICATIONS not granted; worker may skip.")
        }
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) {
            Log.w("TASKS", "Notifications disabled in system settings.")
        }
        val input = Data.Builder().putString("title", title).build()
        val builder = OneTimeWorkRequestBuilder<TaskNotifyWorker>()
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .setInputData(input)
        if (delay <= 15 * 60 * 1000L) builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        val req = builder.build()
        WorkManager.getInstance(context).enqueueUniqueWork("task_$workId", ExistingWorkPolicy.REPLACE, req)
        Log.d("TASKS", "Scheduled title=$title due='$dueStr' delayMs=$delay workId=${req.id}")
    }

    private fun scheduleTestReminder(context: Context) {
        val input = Data.Builder().putString("title", "Test notification").build()
        val req = OneTimeWorkRequestBuilder<TaskNotifyWorker>()
            .setInitialDelay(5, java.util.concurrent.TimeUnit.SECONDS)
            .setConstraints(Constraints.NONE)
            .setInputData(input)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("task_test", ExistingWorkPolicy.REPLACE, req)
        Log.d("TASKS", "Scheduled test notification in 5s id=${req.id}")
    }

    private fun openAppNotifSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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
        var desc by remember { mutableStateOf("Loading…") }
        var comments by remember { mutableStateOf<List<String>>(emptyList()) }
        var newComment by rememberSaveable { mutableStateOf("") }

        DisposableEffect(Unit) {
            val threadRef = DB.fs.collection("threads").document("default")
            threadRef.get().addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    threadRef.set(mapOf("description" to "Welcome to FocuSticks discussion!"), SetOptions.merge())
                }
            }
            val reg1 = threadRef.addSnapshotListener { doc, _ ->
                desc = doc?.getString("description").orEmpty().ifBlank { "No description yet." }
            }
            val reg2 = threadRef.collection("comments").orderBy("createdAt")
                .addSnapshotListener { snap, _ ->
                    comments = snap?.documents?.map { it.getString("text").orEmpty() }.orEmpty()
                }
            onDispose { reg1.remove(); reg2.remove() }
        }

        Scaffold(topBar = { SimpleTopBar(title = "Discussion", onBack = { nav.popBackStack() }) }) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Discussion", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(16.dp))
                Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedCard { Text(desc, modifier = Modifier.padding(12.dp)) }
                Spacer(Modifier.height(20.dp))
                Text("Comments", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedCard {
                    Column(Modifier.padding(12.dp)) {
                        if (comments.isEmpty()) Text("No comments yet.")
                        comments.forEach {
                            Text("• $it")
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = newComment, onValueChange = { newComment = it }, label = { Text("Add a comment") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val data = mapOf(
                            "text" to newComment.trim(),
                            "createdAt" to Timestamp.now(),
                            "uid" to (DB.auth.currentUser?.uid ?: "anon")
                        )
                        if (newComment.isNotBlank()) {
                            DB.fs.collection("threads").document("default").collection("comments").add(data)
                            newComment = ""
                        }
                    },
                    enabled = newComment.isNotBlank()
                ) { Text("Post") }
            }
        }
    }

    @Composable
    private fun StreaksScreen(nav: NavHostController) {
        Scaffold(topBar = { SimpleTopBar(title = "Streaks", onBack = { nav.popBackStack() }) }) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Streaks", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(24.dp))
                Text("Current Streak", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("5 Points")
                Spacer(Modifier.height(28.dp))
                Text("Total tasks completed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("10")
                Spacer(Modifier.height(28.dp))
                Text("View Task", textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { nav.navigate(Route.Task.name) })
            }
        }
    }

    @Composable
    private fun ProfileScreen(nav: NavHostController) {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var showConfirm by remember { mutableStateOf(false) }
        val uid = DB.auth.currentUser?.uid ?: "anon"
        val email = DB.auth.currentUser?.email ?: "guest@example.com"
        var edit by rememberSaveable { mutableStateOf(false) }
        var name by rememberSaveable { mutableStateOf("") }
        var studentId by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }

        DisposableEffect(uid) {
            val ref = DB.fs.collection("users").document(uid)
            val reg = ref.addSnapshotListener { doc, _ ->
                val defaultName = email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                name = doc?.getString("name") ?: defaultName
                studentId = doc?.getString("studentId") ?: "00947890"
                phone = doc?.getString("phone") ?: "+1 (203)0109999"
            }
            onDispose { reg.remove() }
        }

        Scaffold(
            topBar = {
                SimpleTopBar(
                    title = "Profile",
                    onBack = { nav.popBackStack() },
                    actions = {
                        TextButton(onClick = { edit = !edit }) { Text(if (edit) "Done" else "Edit") }
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
                if (edit) {
                    EditableField("Name", name) { name = it }
                    Spacer(Modifier.height(10.dp))
                    EditableField("Student ID", studentId) { studentId = it }
                    Spacer(Modifier.height(10.dp))
                    EditableField("Email", email, enabled = false) {}
                    Spacer(Modifier.height(10.dp))
                    EditableField("Phone no", phone) { phone = it }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val data = mapOf("name" to name.trim(), "studentId" to studentId.trim(), "phone" to phone.trim(), "updatedAt" to Timestamp.now())
                            DB.fs.collection("users").document(uid).set(data, SetOptions.merge())
                            scope.launch { snackbarHostState.showSnackbar("Profile updated") }
                            edit = false
                        },
                        modifier = Modifier.fillMaxWidth(0.5f).height(44.dp)
                    ) { Text("Save") }
                } else {
                    ProfileField(label = "Name", value = name)
                    Spacer(Modifier.height(10.dp))
                    ProfileField(label = "Student ID", value = studentId)
                    Spacer(Modifier.height(10.dp))
                    ProfileField(label = "Email", value = email)
                    Spacer(Modifier.height(10.dp))
                    ProfileField(label = "Phone no", value = phone)
                }
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
                                DB.auth.signOut()
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
    private fun EditableField(label: String, value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(value = value, onValueChange = { onChange(it) }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun ProfileField(label: String, value: String) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            OutlinedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors()) {
                Text(text = value, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    @Composable
    private fun AvatarLarge() {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp, modifier = Modifier.size(96.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = "Avatar", modifier = Modifier.size(72.dp))
            }
        }
    }
}

class TaskNotifyWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Task Reminder"
        val permitted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        if (!permitted) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel("tasks") == null) {
                val ch = NotificationChannel("tasks", "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
                nm.createNotificationChannel(ch)
            }
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(applicationContext, "tasks")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Task Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(title.hashCode(), n)
        Log.d("TASKS", "Notification shown for title=$title")
        return Result.success()
    }
}

private fun showNotificationNow(context: Context, title: String) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) return
    val intent = Intent(context, MainActivity::class.java)
    val pi = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val n = NotificationCompat.Builder(context, "tasks")
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Task Reminder")
        .setContentText(title)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pi)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(title.hashCode(), n)
}
