package com.blogmd.mizukiwriter.ui.feature.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold

@Composable
fun ConfigHubRoute(
    onOpenConfigFile: () -> Unit,
    onOpenConfigDetails: () -> Unit,
) {
    PrimaryScreenScaffold(title = "配置") { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ConfigEntryCard(
                    title = "配置文件",
                    subtitle = "按模块编辑 src/config.ts 中直接生效的全局配置。",
                    onClick = onOpenConfigFile,
                )
            }
            item {
                ConfigEntryCard(
                    title = "配置详情",
                    subtitle = "编辑关于页、友链、日记、项目、技能、时间线、设备、番剧等功能页数据。",
                    onClick = onOpenConfigDetails,
                )
            }
        }
    }
}

@Composable
private fun ConfigEntryCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
