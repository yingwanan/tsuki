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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blogmd.mizukiwriter.domain.MizukiCatalog
import com.blogmd.mizukiwriter.domain.MizukiFeatureDocument
import com.blogmd.mizukiwriter.ui.components.CompactTopBarIconButton
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults

@Composable
fun ConfigDetailsRoute(
    onBack: () -> Unit,
    onOpenDocument: (MizukiFeatureDocument) -> Unit,
) {
    PrimaryScreenScaffold(
        title = "配置详情",
        navigationIcon = {
            CompactTopBarIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
            )
        }
) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(MizukiCatalog.featureDocuments, key = { it.path + (it.bindingName ?: "") }) { item ->
                Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenDocument(item) },
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                        Text("仓库路径：${item.path}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
