package com.blogmd.mizukiwriter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blogmd.mizukiwriter.ui.feature.editor.EditorRoute
import com.blogmd.mizukiwriter.ui.feature.posts.PostsRoute
import com.blogmd.mizukiwriter.ui.feature.settings.SettingsRoute

@Composable
fun MizukiWriterApp() {
    val navController = rememberNavController()
    val tabs = listOf(
        AppDestination.Posts,
        AppDestination.Settings,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route?.startsWith(AppDestination.Editor.baseRoute) != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { destination.matches(it.route) } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Posts.route,
            modifier = if (showBottomBar) Modifier.padding(innerPadding) else Modifier,
        ) {
            composable(AppDestination.Posts.route) {
                PostsRoute(
                    onCreateDraft = { navController.navigate(AppDestination.Editor.createRoute(0L)) },
                    onOpenDraft = { draftId -> navController.navigate(AppDestination.Editor.createRoute(draftId)) },
                )
            }
            composable(
                route = AppDestination.Editor.route,
                arguments = listOf(navArgument("draftId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val draftId = backStackEntry.arguments?.getLong("draftId") ?: 0L
                EditorRoute(
                    draftId = draftId,
                    onBack = { navController.navigateUp() },
                )
            }
            composable(AppDestination.Settings.route) { SettingsRoute() }
        }
    }
}

private fun AppDestination.matches(route: String?): Boolean {
    if (route == null) return false
    return route == this.route || route.startsWith(baseRoute)
}
