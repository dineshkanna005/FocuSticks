package com.example.focusticks.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun BottomBar(nav: NavHostController, route: String?) {

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.height(68.dp)
    ) {

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
            label = { Text("Task") },
            alwaysShowLabel = false
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
            label = { Text("Leaderboard") },
            alwaysShowLabel = false
        )

        NavigationBarItem(
            selected = route == "groups",
            onClick = {
                if (route != "groups") {
                    nav.navigate("groups") {
                        popUpTo("dashboard") { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = { Icon(Icons.Outlined.Chat, null) },
            label = { Text("Discussion") },
            alwaysShowLabel = false
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
            label = { Text("Profile") },
            alwaysShowLabel = false
        )
    }
}
