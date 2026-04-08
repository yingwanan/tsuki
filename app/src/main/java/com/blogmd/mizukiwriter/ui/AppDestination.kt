package com.blogmd.mizukiwriter.ui

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Description
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
    data object Diary : AppDestination("diary", "diary", "日记", Icons.AutoMirrored.Outlined.MenuBook)
    data object Config : AppDestination("config", "config", "配置", Icons.Outlined.Description)
    data object ConfigFile : AppDestination("config-file", "config-file", "配置文件", Icons.Outlined.Description)
    data object ConfigDetails : AppDestination("config-details", "config-details", "配置详情", Icons.Outlined.Description)
    data object Editor : AppDestination("editor/{draftId}", "editor", "编辑", Icons.Outlined.EditNote) {
        fun createRoute(draftId: Long): String = "editor/$draftId"
    }
    data object RepositoryFile : AppDestination(
        "repository-file/{encodedPath}?binding={binding}&title={title}",
        "repository-file",
        "远程文件",
        Icons.Outlined.EditNote,
    ) {
        fun createRoute(path: String, bindingName: String? = null, title: String? = null): String {
            val encodedPath = Uri.encode(path)
            val encodedBinding = Uri.encode(bindingName.orEmpty())
            val encodedTitle = Uri.encode(title.orEmpty())
            return "repository-file/$encodedPath?binding=$encodedBinding&title=$encodedTitle"
        }
    }
    data object Settings : AppDestination("settings", "settings", "设置", Icons.Outlined.Settings)
}
