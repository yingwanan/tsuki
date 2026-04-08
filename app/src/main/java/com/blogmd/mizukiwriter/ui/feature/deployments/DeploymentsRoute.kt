package com.blogmd.mizukiwriter.ui.feature.deployments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.blogmd.mizukiwriter.data.deployment.DeploymentEducationCatalog
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold

@Composable
fun DeploymentsRoute(
    onOpenDeploymentSettings: () -> Unit,
    onOpenDeploymentGuide: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: DeploymentsViewModel = viewModel(
        factory = DeploymentsViewModel.factory(
            settingsRepository = container.settingsRepository,
            deploymentRepository = container.deploymentCenterRepository,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val guide = DeploymentEducationCatalog.guideFor(state.selectedPlatform)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    PrimaryScreenScaffold(title = "部署中心") { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("选择部署平台", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.availablePlatforms.forEach { platform ->
                                FilterChip(
                                    selected = state.selectedPlatform == platform,
                                    onClick = { viewModel.selectPlatform(platform) },
                                    label = { Text(platform.label) },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("GitHub 仓库摘要", style = MaterialTheme.typography.titleMedium)
                        Text("仓库：${state.repositoryName.ifBlank { "未连接" }}", style = MaterialTheme.typography.bodySmall)
                        Text("分支：${state.repositoryBranch.ifBlank { "未配置" }}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "GitHub 仓库和 PAT 仍在一级“设置”里维护；部署中心只展示摘要并依赖这些配置。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = onOpenSettings) {
                            Text("前往 GitHub 设置")
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(state.providerLabel.ifBlank { "部署平台未配置" }, style = MaterialTheme.typography.titleMedium)
                        Text("项目：${state.projectName.ifBlank { "未配置" }}", style = MaterialTheme.typography.bodySmall)
                        if (state.projectId.isNotBlank()) {
                            Text("项目 ID：${state.projectId}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("生产域名：${state.productionDomain.ifBlank { "未返回" }}", style = MaterialTheme.typography.bodySmall)
                        Text("预览域名：${state.previewDomain.ifBlank { "未返回" }}", style = MaterialTheme.typography.bodySmall)
                        if (state.customDomain.isNotBlank()) {
                            Text(
                                "自定义域名：${state.customDomain} (${state.customDomainStatus.ifBlank { "待校验" }})",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (state.setupHint.isNotBlank()) {
                            Text(state.setupHint, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { viewModel.createProject() }, modifier = Modifier.fillMaxWidth()) {
                            Text("创建或校验项目")
                        }
                        if (state.customDomain.isNotBlank()) {
                            Button(onClick = { viewModel.bindCustomDomain() }, modifier = Modifier.fillMaxWidth()) {
                                Text("绑定已填写的自定义域名")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onOpenDeploymentSettings) {
                                Text("打开部署设置")
                            }
                            TextButton(onClick = onOpenDeploymentGuide) {
                                Text("查看部署教程")
                            }
                        }
                        if (state.consoleUrl.isNotBlank()) {
                            TextButton(onClick = { uriHandler.openUri(state.consoleUrl) }) {
                                Text("打开平台控制台")
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("部署总教程", style = MaterialTheme.typography.titleMedium)
                        Text(guide.intro, style = MaterialTheme.typography.bodySmall)
                        guide.sections.take(3).forEachIndexed { index, section ->
                            Text("${index + 1}. ${section.title}", style = MaterialTheme.typography.bodyMedium)
                            Text(section.body, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onOpenDeploymentGuide) {
                            Text("查看完整教程与官方链接")
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("最近部署记录", style = MaterialTheme.typography.titleMedium)
                        if (state.deployments.isEmpty()) {
                            Text("确认仓库已开启 GitHub Actions 或 Pages 工作流。", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            items(state.deployments, key = { it.id }) { deployment ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(deployment.name, style = MaterialTheme.typography.titleMedium)
                        Text("分支：${deployment.branch}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "状态：${deployment.conclusion ?: deployment.status}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("更新时间：${deployment.updatedAt}", style = MaterialTheme.typography.bodySmall)
                        if (deployment.htmlUrl.isNotBlank()) {
                            TextButton(onClick = { uriHandler.openUri(deployment.htmlUrl) }) {
                                Text("打开运行详情")
                            }
                        }
                    }
                }
            }
            state.message?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
