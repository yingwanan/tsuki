package com.blogmd.mizukiwriter.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val baseRoute: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Posts : AppDestination("posts", "posts", "文章", Icons.AutoMirrored.Outlined.Article)
    data object Editor : AppDestination("editor/{draftId}", "editor", "编辑", Icons.Outlined.EditNote) {
        fun createRoute(draftId: Long): String = "editor/$draftId"
    }
    data object Settings : AppDestination("settings", "settings", "设置", Icons.Outlined.Settings)
}
