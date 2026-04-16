package com.blogmd.mizukiwriter.ui.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults

@Composable
fun DashboardRoute() {
    val container = LocalContext.current.appContainer
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(
            settingsRepository = container.settingsRepository,
            draftRepository = container.draftRepository,
            deploymentRepository = container.deploymentCenterRepository,
        )
)
    val state by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    PrimaryScreenScaffold(title = "博客控制台") { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SummaryCard(
                    title = "仓库",
                    lines = listOf(
                        "仓库：${state.repositoryName.ifBlank { "未连接" }}",
                        "默认分支：${state.defaultBranch.ifBlank { "未知" }}",
                    ),
                )
            }
            item {
                SummaryCard(
                    title = "内容",
                    lines = listOf("本地草稿：${state.localDraftCount}"),
                )
            }
            item {
                SummaryCard(
                    title = "部署",
                    lines = listOf(
                        "平台：${state.providerLabel.ifBlank { "未配置" }}",
                        "生产域名：${state.productionDomain.ifBlank { "未返回" }}",
                        "最近任务：${state.latestDeploymentName.ifBlank { "暂无记录" }}",
                        "状态：${state.latestDeploymentStatus.ifBlank { "未知" }}",
                    ),
                    actionText = if (state.latestDeploymentUrl.isNotBlank()) "打开部署详情" else null,
                    onAction = {
                        if (state.latestDeploymentUrl.isNotBlank()) {
                            uriHandler.openUri(state.latestDeploymentUrl)
                        }
                    },
                )
            }
            state.message?.let { message ->
                item {
                    SummaryCard(title = "提示", lines = listOf(message))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    lines: List<String>,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
            if (actionText != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionText)
                }
            }
        }
    }
}
