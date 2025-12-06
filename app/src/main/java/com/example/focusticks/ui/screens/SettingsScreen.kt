package com.example.focusticks.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavHostController, openDrawer: () -> Unit) {

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                }

                IconButton(onClick = { openDrawer() }) {
                    Icon(Icons.Filled.Menu, null)
                }
            }
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp)
        ) {

            Text("Theme", fontSize = 18.sp)
            Text("Light / Dark mode", modifier = Modifier.padding(bottom = 20.dp))

            Text("Profile", fontSize = 18.sp)
            Text("Change name, course, year", modifier = Modifier.padding(bottom = 20.dp))

            Text("Notifications", fontSize = 18.sp)
            Text("Task reminders and app alerts", modifier = Modifier.padding(bottom = 20.dp))
        }
    }
}
