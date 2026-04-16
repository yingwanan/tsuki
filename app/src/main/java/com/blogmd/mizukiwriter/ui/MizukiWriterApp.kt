package com.blogmd.mizukiwriter.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.domain.MizukiFeatureDocument
import com.blogmd.mizukiwriter.ui.feature.config.ConfigDetailsRoute
import com.blogmd.mizukiwriter.ui.feature.config.ConfigFileRoute
import com.blogmd.mizukiwriter.ui.feature.config.ConfigHubRoute
import com.blogmd.mizukiwriter.ui.feature.content.ContentRoute
import com.blogmd.mizukiwriter.ui.feature.diary.DiaryRoute
import com.blogmd.mizukiwriter.ui.feature.editor.EditorRoute
import com.blogmd.mizukiwriter.ui.feature.repositoryfile.RepositoryFileRoute
import com.blogmd.mizukiwriter.ui.feature.settings.SettingsRoute
import com.blogmd.mizukiwriter.ui.theme.appBackgroundImageScrim
import com.blogmd.mizukiwriter.ui.theme.appChromeContainerColor

@Composable
fun MizukiWriterApp() {
    val container = LocalContext.current.appContainer
    val settings by container.settingsRepository.settings.collectAsState(initial = GitHubSettings())
    val navController = rememberNavController()
    val isDarkTheme = isSystemInDarkTheme()
    val tabs = listOf(
        AppDestination.Posts,
        AppDestination.Diary,
        AppDestination.Config,
        AppDestination.Settings,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = tabs.any { destination -> destination.matches(currentDestination?.route) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (settings.backgroundImagePath.isNotBlank()) {
            AsyncImage(
                model = settings.backgroundImagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (isDarkTheme) 0.78f else 0.9f,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBackgroundImageScrim(isDarkTheme)),
            )
        }
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = appChromeContainerColor(isDarkTheme),
                        tonalElevation = 8.dp,
                        shadowElevation = 4.dp,
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(72.dp),
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0),
                        ) {
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
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
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
