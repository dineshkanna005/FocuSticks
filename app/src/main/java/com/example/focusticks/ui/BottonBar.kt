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
                nav.navigate("task") {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Outlined.Task, contentDescription = null) },
            label = { Text("Task") }
        )
        NavigationBarItem(
            selected = route == "leaderboard",
            onClick = {
                nav.navigate("leaderboard") {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Outlined.Leaderboard, contentDescription = null) },
            label = { Text("Leaderboard") }
        )
        NavigationBarItem(
            selected = route == "discussion",
            onClick = {
                nav.navigate("discussion") {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Outlined.Chat, contentDescription = null) },
            label = { Text("Discussion") }
        )
        NavigationBarItem(
            selected = route == "profile",
            onClick = {
                nav.navigate("profile") {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            label = { Text("Profile") }
        )
    }
}
