package com.example.focusticks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.focusticks.ui.BottomBar
import com.example.focusticks.ui.screens.SplashScreen
import com.example.focusticks.ui.screens.LoginScreen
import com.example.focusticks.ui.screens.SignupScreen
import com.example.focusticks.ui.screens.DashboardScreen
import com.example.focusticks.ui.screens.DiscussionScreen
import com.example.focusticks.ui.screens.LeaderboardScreen
import com.example.focusticks.ui.screens.ProfileScreen
import com.example.focusticks.ui.screens.ForgotPasswordScreen
import com.example.focusticks.ui.screens.StreakScreen
import com.example.focusticks.ui.screens.task.AddTaskScreen
import com.example.focusticks.ui.screens.task.TaskScreen
import com.example.focusticks.ui.screens.task.ScanNotesScreen
import com.example.focusticks.ui.screens.task.CompletedTaskScreen
import com.example.focusticks.ui.screens.task.SmartReminderScreen
import com.example.focusticks.ui.theme.FocuSticksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openTaskId = intent.getStringExtra("openTaskId")
        setContent {
            FocuSticksTheme {
                AppNavigation(openTaskId)
            }
        }
    }
}

@Composable
fun AppNavigation(openTaskId: String?) {

    val nav = rememberNavController()
    val backStack = nav.currentBackStackEntryAsState()
    val route = backStack.value?.destination?.route

    LaunchedEffect(openTaskId) {
        if (openTaskId != null) {
            nav.navigate("task") {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (route != "splash" && route != "login" && route != "signup" && route != "forgot") {
                BottomBar(nav, route)
            }
        }
    ) { padding ->

        NavHost(
            navController = nav,
            startDestination = "splash",
            modifier = Modifier.padding(padding)
        ) {

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
            composable("dashboard") { DashboardScreen(nav) }
            composable("task") { TaskScreen(nav) }
            composable("task_add") { AddTaskScreen(nav) }
            composable("addTask") { AddTaskScreen(nav) }
            composable("scanNotes") { ScanNotesScreen(nav) }
            composable("task_completed") { CompletedTaskScreen(nav) }
            composable("leaderboard") { LeaderboardScreen(nav) }
            composable("discussion") { DiscussionScreen(nav) }
            composable("profile") { ProfileScreen(nav) }
            composable("streak") { StreakScreen(nav) }
            composable("smartReminder") { SmartReminderScreen(nav) }
        }
    }
}
