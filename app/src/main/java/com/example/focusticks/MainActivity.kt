package com.example.focusticks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.focusticks.ui.BottomBar
import com.example.focusticks.ui.screens.*
import com.example.focusticks.ui.screens.task.*
import com.example.focusticks.ui.theme.FocuSticksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val openTaskId = intent.getStringExtra("openTaskId")
        val openType = intent.getStringExtra("openType")

        setContent {
            FocuSticksTheme {
                AppNavigation(openTaskId, openType)
            }
        }
    }
}

@Composable
fun AppNavigation(openTaskId: String?, openType: String?) {

    val nav = rememberNavController()

    LaunchedEffect(openTaskId, openType) {
        if (openTaskId != null) {
            when (openType) {
                "completed" -> nav.navigate("task_completed")
                else -> nav.navigate("task")
            }
        }
    }

    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (route !in listOf("splash", "login", "signup", "forgot")) {
                BottomBar(nav, route)
            }
        }
    ) { pad ->

        NavHost(
            navController = nav,
            startDestination = "splash",
            modifier = Modifier.padding(pad)
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

            composable("task") {
                TaskScreen(nav, openTaskId, openType)
            }

            composable("addTask") { AddTaskScreen(nav) }
            composable("scanNotes") { ScanNotesScreen(nav) }

            composable("task_completed") {
                CompletedTaskScreen(nav, openTaskId, openType)
            }

            composable("smartReminder") { SmartReminderScreen(nav) }
            composable("leaderboard") { LeaderboardScreen(nav) }
            composable("profile") { ProfileScreen(nav) }
            composable("streak") { StreakScreen(nav) }
            composable("discussion") { DiscussionScreen(nav) }

            composable("discussion_comments/{id}") { backEntry ->
                val id = backEntry.arguments?.getString("id") ?: ""
                CommentScreen(nav, id)
            }
        }
    }
}
