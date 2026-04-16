package com.blogmd.mizukiwriter.ui.feature.deployments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.data.deployment.DeploymentEducationCatalog
import com.blogmd.mizukiwriter.data.deployment.DeploymentFieldDoc
import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform
import com.blogmd.mizukiwriter.data.settings.EdgeOneExecutionMode
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.ui.appContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeploymentSettingsRoute(
    onBack: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenGlobalSettings: () -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: DeploymentSettingsViewModel = viewModel(
        factory = DeploymentSettingsViewModel.factory(container.settingsRepository)
)
    val state by viewModel.uiState.collectAsState()
    val savedMessage by viewModel.savedMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val docs = remember(state.deploymentPlatform) {
        DeploymentEducationCatalog.fieldDocsFor(state.deploymentPlatform).associateBy { it.label }
    }
    val formKey = remember(state) {
        listOf(
            state.deploymentPlatform.name,
            state.deploymentAccessToken,
            state.deploymentProjectName,
            state.deploymentProjectId,
            state.deploymentAccountId,
            state.deploymentTeamId,
            state.deploymentCustomDomain,
            state.deploymentWorkflow,
            state.deploymentBuildCommand,
            state.deploymentOutputDirectory,
            state.edgeOneExecutionMode.name,
        ).joinToString("|")
    }
    var deploymentAccessToken by rememberSaveable(formKey) { mutableStateOf(state.deploymentAccessToken) }
    var deploymentProjectName by rememberSaveable(formKey) { mutableStateOf(state.deploymentProjectName) }
    var deploymentProjectId by rememberSaveable(formKey) { mutableStateOf(state.deploymentProjectId) }
    var deploymentAccountId by rememberSaveable(formKey) { mutableStateOf(state.deploymentAccountId) }
    var deploymentTeamId by rememberSaveable(formKey) { mutableStateOf(state.deploymentTeamId) }
    var deploymentCustomDomain by rememberSaveable(formKey) { mutableStateOf(state.deploymentCustomDomain) }
    var deploymentWorkflow by rememberSaveable(formKey) { mutableStateOf(state.deploymentWorkflow) }
    var deploymentBuildCommand by rememberSaveable(formKey) { mutableStateOf(state.deploymentBuildCommand) }
    var deploymentOutputDirectory by rememberSaveable(formKey) { mutableStateOf(state.deploymentOutputDirectory) }
    var edgeOneExecutionMode by rememberSaveable(formKey) { mutableStateOf(state.edgeOneExecutionMode) }

    LaunchedEffect(savedMessage) {
        savedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSavedMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("部署设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
            item {
                Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("当前平台：${state.deploymentPlatform.label}", style = MaterialTheme.typography.titleMedium)
                        Text("在部署中心首页切换平台；这里会只显示当前平台相关参数。", style = MaterialTheme.typography.bodySmall)
                        Text("当前项目：${state.deploymentProjectName.ifBlank { "未填写" }}", style = MaterialTheme.typography.bodySmall)
                        Text("当前自定义域名：${state.deploymentCustomDomain.ifBlank { "未填写" }}", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onOpenGlobalSettings) {
                            Text("GitHub 与仓库配置仍在一级设置页")
                        }
                        TextButton(onClick = onOpenGuide) {
                            Text("查看完整部署教程")
                        }
                    }
                }
            }
            when (state.deploymentPlatform) {
                DeploymentPlatform.Vercel -> {
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("部署平台 Token"),
                            value = deploymentAccessToken,
                            onValueChange = { deploymentAccessToken = it },
                            uriHandler = uriHandler,
                            password = true,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("部署项目名"),
                            value = deploymentProjectName,
                            onValueChange = { deploymentProjectName = it },
                            uriHandler = uriHandler,
                        )
                    }
                    item {
                        SettingFieldCard(
                            label = "部署项目 ID",
                            value = deploymentProjectId,
                            onValueChange = { deploymentProjectId = it },
                            hint = "已有项目时可手动补充；首次创建项目可以留空。",
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("平台 Team ID"),
                            value = deploymentTeamId,
                            onValueChange = { deploymentTeamId = it },
                            uriHandler = uriHandler,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("构建命令"),
                            value = deploymentBuildCommand,
                            onValueChange = { deploymentBuildCommand = it },
                            uriHandler = uriHandler,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("产物目录"),
                            value = deploymentOutputDirectory,
                            onValueChange = { deploymentOutputDirectory = it },
                            uriHandler = uriHandler,
                        )
                    }
                }

                DeploymentPlatform.CloudflarePages -> {
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("部署平台 Token"),
                            value = deploymentAccessToken,
                            onValueChange = { deploymentAccessToken = it },
                            uriHandler = uriHandler,
                            password = true,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("平台 Account ID"),
                            value = deploymentAccountId,
                            onValueChange = { deploymentAccountId = it },
                            uriHandler = uriHandler,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("部署项目名"),
                            value = deploymentProjectName,
                            onValueChange = { deploymentProjectName = it },
                            uriHandler = uriHandler,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("构建命令"),
                            value = deploymentBuildCommand,
                            onValueChange = { deploymentBuildCommand = it },
                            uriHandler = uriHandler,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("产物目录"),
                            value = deploymentOutputDirectory,
                            onValueChange = { deploymentOutputDirectory = it },
                            uriHandler = uriHandler,
                        )
                    }
                }

                DeploymentPlatform.EdgeOnePages -> {
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("部署项目名"),
                            value = deploymentProjectName,
                            onValueChange = { deploymentProjectName = it },
                            uriHandler = uriHandler,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("部署平台 Token"),
                            value = deploymentAccessToken,
                            onValueChange = { deploymentAccessToken = it },
                            uriHandler = uriHandler,
                            password = true,
                        )
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("部署工作流名"),
                            value = deploymentWorkflow,
                            onValueChange = { deploymentWorkflow = it },
                            uriHandler = uriHandler,
                        )
                    }
                    item {
                        Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("EdgeOne 执行方式", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = edgeOneExecutionMode == EdgeOneExecutionMode.GitHubActions,
                                        onClick = { edgeOneExecutionMode = EdgeOneExecutionMode.GitHubActions },
                                        label = { Text("GitHub Actions") },
                                    )
                                    FilterChip(
                                        selected = edgeOneExecutionMode == EdgeOneExecutionMode.LimitedLocalCli,
                                        onClick = { edgeOneExecutionMode = EdgeOneExecutionMode.LimitedLocalCli },
                                        label = { Text("受限本地 CLI") },
                                    )
                                }
                                Text(
                                    "推荐使用 GitHub Actions。若你未明确验证过本地 CLI 运行环境，不要选受限本地 CLI。",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    item {
                        DeploymentDocField(
                            doc = docs.getValue("构建命令"),
                            value = deploymentBuildCommand,
                            onValueChange = { deploymentBuildCommand = it },
                            uriHandler = uriHandler,
                        )
                    }
                }
            }
            item {
                SettingFieldCard(
                    label = "自定义域名",
                    value = deploymentCustomDomain,
                    onValueChange = { deploymentCustomDomain = it },
                    hint = "填写准备绑定的域名；创建项目成功后回到部署中心执行绑定。",
                )
            }
            item {
                TextButton(
                    onClick = {
                        viewModel.save(
                            state.copy(
                                deploymentAccessToken = deploymentAccessToken,
                                deploymentProjectName = deploymentProjectName,
                                deploymentProjectId = deploymentProjectId,
                                deploymentAccountId = deploymentAccountId,
                                deploymentTeamId = deploymentTeamId,
                                deploymentCustomDomain = deploymentCustomDomain,
                                deploymentWorkflow = deploymentWorkflow,
                                deploymentBuildCommand = deploymentBuildCommand,
                                deploymentOutputDirectory = deploymentOutputDirectory,
                                edgeOneExecutionMode = edgeOneExecutionMode,
                            ),
                        )
                    },
                ) {
                    Text("保存部署设置")
                }
            }
        }
    }
}

@Composable
private fun DeploymentDocField(
    doc: DeploymentFieldDoc,
    value: String,
    onValueChange: (String) -> Unit,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    password: Boolean = false,
) {
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(doc.label, style = MaterialTheme.typography.titleMedium)
            Text(doc.help, style = MaterialTheme.typography.bodySmall)
            Text("示例：${doc.example}", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(doc.label) },
                visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                singleLine = true,
            )
            TextButton(onClick = { uriHandler.openUri(doc.referenceUrl) }) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                Text(doc.referenceLabel, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun SettingFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
) {
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                supportingText = { Text(hint) },
                singleLine = true,
            )
        }
    }
}
