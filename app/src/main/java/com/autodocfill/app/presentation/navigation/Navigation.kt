package com.autodocfill.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autodocfill.app.presentation.document.DocumentListScreen
import com.autodocfill.app.presentation.document.ManualEditScreen
import com.autodocfill.app.presentation.profile.ProfileListScreen

/**
 * Main navigation component with bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDocFillNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Hide bottom bar on certain screens
    val showBottomBar = currentDestination?.route?.startsWith("manual_edit") != true
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.values().forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Documents.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Documents.route) {
                DocumentListScreen(navController)
            }
            composable(Screen.Profile.route) {
                ProfileListScreen(navController)
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = "manual_edit/{documentId}",
                arguments = listOf(navArgument("documentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                ManualEditScreen(
                    documentId = documentId,
                    navController = navController
                )
            }
        }
    }
}

/**
 * App navigation screens
 */
enum class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Documents("documents", "Documents", Icons.Filled.Description),
    Profile("profile", "Profile", Icons.Filled.Person),
    History("history", "History", Icons.Filled.History),
    Settings("settings", "Settings", Icons.Filled.Settings)
}

/**
 * Placeholder screens
 */
@Composable
fun HistoryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "History Screen - Coming Soon",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Settings Screen - Coming Soon",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

