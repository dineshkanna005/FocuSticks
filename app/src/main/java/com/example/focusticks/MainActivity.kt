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
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.example.focusticks.ui.BottomBar
import com.example.focusticks.ui.screens.*
import com.example.focusticks.ui.screens.task.*
import com.example.focusticks.ui.theme.FocuSticksTheme
import com.example.focusticks.ui.screens.posts.PostsFeedScreen
import com.example.focusticks.ui.screens.posts.CreatePostScreen
import com.example.focusticks.ui.screens.posts.PostCommentsScreen
import com.example.focusticks.ui.screens.discussion.GroupsListScreen
import com.example.focusticks.ui.screens.discussion.CreateGroupScreen
import com.example.focusticks.ui.screens.discussion.JoinGroupScreen
import com.example.focusticks.ui.screens.discussion.GroupChatScreen
import com.example.focusticks.ui.screens.friends.FriendsScreen
import com.example.focusticks.ui.screens.friends.FriendTasksScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deepTaskId = intent.getStringExtra("openTaskId")
        val deepTaskType = intent.getStringExtra("openType")

        setContent {
            val auth = FirebaseAuth.getInstance()
            var userId by remember { mutableStateOf(auth.currentUser?.uid) }
            var isDark by remember { mutableStateOf(false) }
            var themeLoaded by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { a ->
                    userId = a.currentUser?.uid
                }
                auth.addAuthStateListener(listener)
                onDispose { auth.removeAuthStateListener(listener) }
            }

            LaunchedEffect(userId) {
                if (userId != null) {
                    Firebase.firestore.collection("users").document(userId!!)
                        .get()
                        .addOnSuccessListener {
                            isDark = (it.getString("theme") == "dark")
                            themeLoaded = true
                        }
                        .addOnFailureListener {
                            isDark = false
                            themeLoaded = true
                        }
                } else {
                    isDark = false
                    themeLoaded = true
                }
            }

            if (themeLoaded) {
                FocuSticksTheme(darkTheme = isDark) {
                    CompositionLocalProvider(
                        LocalThemeReloader provides {
                            Firebase.firestore.collection("users").document(userId!!)
                                .get()
                                .addOnSuccessListener {
                                    isDark = (it.getString("theme") == "dark")
                                }
                        }
                    ) {
                        AppNavigation(deepTaskId, deepTaskType)
                    }
                }
            }
        }
    }
}

val LocalThemeReloader = compositionLocalOf<(() -> Unit)> { error("") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(openTaskId: String?, openType: String?) {

    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    var showAlert by remember { mutableStateOf(false) }
    var alertMsg by remember { mutableStateOf("") }
    var alertId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            Firebase.firestore.collection("users")
                .document(uid)
                .collection("alerts")
                .whereEqualTo("seen", false)
                .get()
                .addOnSuccessListener { snap ->
                    if (!snap.isEmpty) {
                        val doc = snap.documents.first()
                        alertMsg = doc.getString("message") ?: ""
                        alertId = doc.id
                        showAlert = true
                    }
                }
        }

        if (openTaskId != null) {
            if (openType == "follower") {
                nav.navigate("friends?highlightUid=$openTaskId") {
                    popUpTo(0)
                }
            } else {
                nav.navigate("task?openTaskId=$openTaskId&openType=$openType") {
                    popUpTo(0)
                }
            }
        }
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text("New Follower!") },
            text = { Text(alertMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            Firebase.firestore.collection("users")
                                .document(uid)
                                .collection("alerts")
                                .document(alertId)
                                .update("seen", true)
                        }
                        showAlert = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    val drawerItems = listOf(
        "posts" to "Posts Feed",
        "profile" to "Profile",
        "friends" to "Friends",
        "calendar" to "Calendar",
        "notes" to "Notes",
        "achievements" to "Achievements",
        "settings" to "Settings"
    )

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column {
                    drawerItems.forEach {
                        NavigationDrawerItem(
                            label = { Text(it.second) },
                            selected = currentRoute.startsWith(it.first),
                            onClick = {
                                scope.launch { drawerState.close() }
                                nav.navigate(it.first) {
                                    popUpTo("dashboard")
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .clickable {
                                scope.launch { drawerState.close() }
                                FirebaseAuth.getInstance().signOut()
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
                if (currentRoute !in listOf("splash", "login", "signup", "forgot"))
                    BottomBar(nav, currentRoute)
            }
        ) { pad ->
            Surface(modifier = Modifier.padding(pad)) {
                NavHost(nav, startDestination = "splash") {

                    composable("splash") {
                        SplashScreen {
                            nav.navigate(it) {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }

                    composable("login") { LoginScreen(nav) }
                    composable("signup") { SignupScreen(nav) }
                    composable("forgot") { ForgotPasswordScreen(nav) }
                    composable("dashboard") { DashboardScreen(nav, openDrawer) }

                    composable(
                        "task?openTaskId={openTaskId}&openType={openType}",
                        arguments = listOf(
                            navArgument("openTaskId") { nullable = true; type = NavType.StringType },
                            navArgument("openType") { nullable = true; type = NavType.StringType }
                        )
                    ) {
                        TaskScreen(
                            nav,
                            it.arguments?.getString("openTaskId"),
                            it.arguments?.getString("openType"),
                            openDrawer
                        )
                    }

                    composable("addTask") { AddTaskScreen(nav, openDrawer) }
                    composable("scanNotes") { ScanNotesScreen(nav, openDrawer) }

                    composable(
                        "task_completed?openTaskId={openTaskId}&openType={openType}",
                        arguments = listOf(
                            navArgument("openTaskId") { nullable = true; type = NavType.StringType },
                            navArgument("openType") { nullable = true; type = NavType.StringType }
                        )
                    ) {
                        CompletedTaskScreen(
                            nav,
                            it.arguments?.getString("openTaskId"),
                            it.arguments?.getString("openType"),
                            openDrawer
                        )
                    }

                    composable("leaderboard") { LeaderboardScreen(nav, openDrawer) }
                    composable("profile") { ProfileScreen(nav, openDrawer) }

                    composable(
                        "friends?highlightUid={highlightUid}",
                        arguments = listOf(
                            navArgument("highlightUid") { nullable = true; type = NavType.StringType }
                        )
                    ) {
                        FriendsScreen(nav, openDrawer)
                    }

                    composable(
                        "friendTasks/{uid}",
                        arguments = listOf(navArgument("uid") { type = NavType.StringType })
                    ) {
                        FriendTasksScreen(nav, it.arguments?.getString("uid"))
                    }

                    composable("streak") { StreakScreen(nav, openDrawer) }
                    composable("calendar") { CalendarScreen(nav, openDrawer) }
                    composable("notes") { NotesScreen(nav, openDrawer) }
                    composable("achievements") { AchievementsScreen(nav, openDrawer) }
                    composable("settings") { SettingsScreen(nav, openDrawer) }
                    composable("smartReminder") { SmartReminderScreen(nav, openDrawer) }

                    composable("posts") { PostsFeedScreen(nav, openDrawer) }
                    composable("createPost") { CreatePostScreen(nav, openDrawer) }

                    composable(
                        "postComments/{postId}",
                        arguments = listOf(
                            navArgument("postId") { type = NavType.StringType }
                        )
                    ) {
                        PostCommentsScreen(
                            nav,
                            it.arguments?.getString("postId") ?: "",
                            openDrawer
                        )
                    }

                    composable("groups") { GroupsListScreen(nav, openDrawer) }
                    composable("createGroup") { CreateGroupScreen(nav, openDrawer) }
                    composable("joinGroup") { JoinGroupScreen(nav, openDrawer) }

                    composable(
                        "groupChat/{groupId}",
                        arguments = listOf(
                            navArgument("groupId") { type = NavType.StringType }
                        )
                    ) {
                        GroupChatScreen(
                            nav,
                            it.arguments?.getString("groupId") ?: ""
                        )
                    }
                }
            }
        }
    }
}
