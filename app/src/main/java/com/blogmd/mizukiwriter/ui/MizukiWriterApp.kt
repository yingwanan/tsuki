package com.blogmd.mizukiwriter.ui

import android.net.Uri
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.blogmd.mizukiwriter.domain.MizukiFeatureDocument
import com.blogmd.mizukiwriter.ui.feature.config.ConfigDetailsRoute
import com.blogmd.mizukiwriter.ui.feature.config.ConfigFileRoute
import com.blogmd.mizukiwriter.ui.feature.config.ConfigHubRoute
import com.blogmd.mizukiwriter.ui.feature.content.ContentRoute
import com.blogmd.mizukiwriter.ui.feature.diary.DiaryRoute
import com.blogmd.mizukiwriter.ui.feature.editor.EditorRoute
import com.blogmd.mizukiwriter.ui.feature.repositoryfile.RepositoryFileRoute
import com.blogmd.mizukiwriter.ui.feature.settings.SettingsRoute

@Composable
fun MizukiWriterApp() {
    val navController = rememberNavController()
    val tabs = listOf(
        AppDestination.Posts,
        AppDestination.Diary,
        AppDestination.Config,
        AppDestination.Settings,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = tabs.any { destination -> destination.matches(currentDestination?.route) }

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
                ContentRoute(
                    onCreateDraft = { navController.navigate(AppDestination.Editor.createRoute(0L)) },
                    onOpenDraft = { draftId -> navController.navigate(AppDestination.Editor.createRoute(draftId)) },
                    onOpenRemoteFile = { path, title ->
                        navController.navigate(AppDestination.EditorRemote.createRoute(path = path, title = title))
                    },
                )
            }
            composable(AppDestination.Diary.route) {
                DiaryRoute()
            }
            composable(AppDestination.Config.route) {
                ConfigHubRoute(
                    onOpenConfigFile = { navController.navigate(AppDestination.ConfigFile.route) },
                    onOpenConfigDetails = { navController.navigate(AppDestination.ConfigDetails.route) },
                )
            }
            composable(AppDestination.ConfigFile.route) {
                ConfigFileRoute(
                    onBack = { navController.navigateUp() },
                    onOpenExport = { title, bindingName ->
                        navController.navigate(
                            AppDestination.RepositoryFile.createRoute(
                                path = "src/config.ts",
                                bindingName = bindingName,
                                title = title,
                            ),
                        )
                    },
                )
            }
            composable(AppDestination.ConfigDetails.route) {
                ConfigDetailsRoute(
                    onBack = { navController.navigateUp() },
                    onOpenDocument = { document ->
                        navController.navigate(document.toRepositoryRoute())
                    },
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
            composable(
                route = AppDestination.EditorRemote.route,
                arguments = listOf(
                    navArgument("encodedPath") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("encodedPath").orEmpty()
                val title = backStackEntry.arguments?.getString("title").orEmpty().ifBlank { null }
                EditorRoute(
                    draftId = 0L,
                    remoteArticlePath = Uri.decode(encodedPath),
                    remoteArticleTitle = title?.let(Uri::decode),
                    onBack = { navController.navigateUp() },
                )
            }
            composable(
                route = AppDestination.RepositoryFile.route,
                arguments = listOf(
                    navArgument("encodedPath") { type = NavType.StringType },
                    navArgument("binding") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("encodedPath").orEmpty()
                val binding = backStackEntry.arguments?.getString("binding").orEmpty().ifBlank { null }
                val title = backStackEntry.arguments?.getString("title").orEmpty().ifBlank { null }
                RepositoryFileRoute(
                    path = Uri.decode(encodedPath),
                    bindingName = binding?.let(Uri::decode),
                    title = title?.let(Uri::decode),
                    onBack = { navController.navigateUp() },
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsRoute()
            }
        }
    }
}

private fun AppDestination.matches(route: String?): Boolean {
    if (route == null) return false
    return route == this.route || route.startsWith(baseRoute)
}

private fun MizukiFeatureDocument.toRepositoryRoute(): String = AppDestination.RepositoryFile.createRoute(
    path = path,
    bindingName = bindingName,
    title = title,
)
