package com.blogmd.mizukiwriter.ui.feature.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blogmd.mizukiwriter.domain.MizukiCatalog
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold

@Composable
fun ConfigFileRoute(
    onBack: () -> Unit,
    onOpenExport: (String, String) -> Unit,
) {
    PrimaryScreenScaffold(
        title = "配置文件",
        actions = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(MizukiCatalog.configExports, key = { it.bindingName }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenExport(item.title, item.bindingName) },
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                        Text("配置代号：${item.bindingName}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
