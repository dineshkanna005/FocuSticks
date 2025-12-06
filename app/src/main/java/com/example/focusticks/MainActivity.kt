package com.example.focusticks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.navigation.compose.*
import com.example.focusticks.ui.BottomBar
import com.example.focusticks.ui.screens.*
import com.example.focusticks.ui.screens.subject.SubjectsScreen
import com.example.focusticks.ui.screens.task.*
import com.example.focusticks.ui.theme.FocuSticksTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Firebase.firestore.clearPersistence()
            .addOnSuccessListener { startApp() }
            .addOnFailureListener { startApp() }
    }

    private fun startApp() {
        val openTaskId = intent.getStringExtra("openTaskId")
        val openType = intent.getStringExtra("openType")

        setContent {
            FocuSticksTheme {
                AppNavigation(openTaskId, openType)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(openTaskId: String?, openType: String?) {

    val nav = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    val drawerItems = listOf(
        "profile" to "👤 Profile",
        "calendar" to "📅 Calendar",
        "notes" to "📝 Notes",
        "subjects" to "📚 Subjects",
        "settings" to "⚙ Settings"
    )

    LaunchedEffect(openTaskId, openType) {
        if (openTaskId != null) {
            when (openType) {
                "completed" -> nav.navigate("task_completed")
                else -> nav.navigate("task")
            }
        }
    }

    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column {

                    drawerItems.forEach {
                        NavigationDrawerItem(
                            label = { Text(it.second) },
                            selected = route == it.first,
                            onClick = {
                                scope.launch { drawerState.close() }
                                nav.navigate(it.first)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .clickable {
                                scope.launch {
                                    drawerState.close()
                                }
                                Firebase.auth.signOut()
                                nav.navigate("login") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Logout, null, tint = Color.Red)
                        Text("Logout", color = Color.Red, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (route !in listOf("splash", "login", "signup", "forgot"))
                    BottomBar(nav, route)
            }
        ) { pad ->
            Surface(modifier = Modifier.padding(pad)) {

                NavHost(navController = nav, startDestination = "splash") {

                    composable("splash") {
                        SplashScreen { next ->
                            nav.navigate(next) {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }

                    composable("login") { LoginScreen(nav) }
                    composable("signup") { SignupScreen(nav) }
                    composable("forgot") { ForgotPasswordScreen(nav) }
                    composable("dashboard") { DashboardScreen(nav, openDrawer) }

                    composable("task") { TaskScreen(nav, openTaskId, openType, openDrawer) }
                    composable("addTask") { AddTaskScreen(nav, openDrawer) }
                    composable("scanNotes") { ScanNotesScreen(nav, openDrawer) }
                    composable("task_completed") {
                        CompletedTaskScreen(nav, openTaskId, openType, openDrawer)
                    }
                    composable("smartReminder") { SmartReminderScreen(nav, openDrawer) }
                    composable("leaderboard") { LeaderboardScreen(nav, openDrawer) }
                    composable("profile") { ProfileScreen(nav, openDrawer) }
                    composable("streak") { StreakScreen(nav, openDrawer) }
                    composable("discussion") { DiscussionScreen(nav, openDrawer) }
                    composable("calendar") { CalendarScreen(nav, openDrawer) }
                    composable("notes") { NotesScreen(nav, openDrawer) }
                    composable("subjects") { SubjectsScreen(nav, openDrawer) }
                    composable("settings") { SettingsScreen(nav, openDrawer) }
                }
            }
        }
    }
}
