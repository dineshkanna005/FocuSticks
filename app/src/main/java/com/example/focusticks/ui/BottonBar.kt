package com.example.focusticks.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun BottomBar(nav: NavHostController, route: String?) {

    NavigationBar {

        NavigationBarItem(
            selected = route == "task",
            onClick = {
                if (route != "task") {
                    nav.navigate("task") {
                        popUpTo("dashboard") { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = { Icon(Icons.Outlined.Task, null) },
            label = { Text("Task") }
        )

        NavigationBarItem(
            selected = route == "leaderboard",
            onClick = {
                if (route != "leaderboard") {
                    nav.navigate("leaderboard") {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Outlined.Leaderboard, null) },
            label = { Text("Leaderboard") }
        )

        NavigationBarItem(
            selected = route == "topics",
            onClick = {
                if (route != "topics") {
                    nav.navigate("topics") {
                        popUpTo("dashboard") { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = { Icon(Icons.Outlined.Chat, null) },
            label = { Text("Discussion") }
        )

        NavigationBarItem(
            selected = route == "profile",
            onClick = {
                if (route != "profile") {
                    nav.navigate("profile") {
                        popUpTo("dashboard") { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = { Icon(Icons.Outlined.Person, null) },
            label = { Text("Profile") }
        )
    }
}
