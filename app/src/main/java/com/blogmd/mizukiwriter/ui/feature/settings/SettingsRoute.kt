package com.blogmd.mizukiwriter.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.WorkspaceMode
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold

private const val GITHUB_FINE_GRAINED_TOKEN_URL = "https://github.com/settings/personal-access-tokens/new"
private const val GITHUB_PAT_DOCS_URL = "https://docs.github.com/zh/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens"
private const val GITHUB_MIZUKI_REPO_URL = "https://github.com/matsuzaka-yuki/Mizuki"
private const val MIZUKI_DOCS_URL = "https://docs.mizuki.mysqil.com/"

@Composable
fun SettingsRoute() {
    val container = LocalContext.current.appContainer
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container.settingsRepository))
    val state by viewModel.uiState.collectAsState()
    val savedMessage by viewModel.savedMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(savedMessage) {
        savedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSavedMessage()
        }
    }

    val formKey = remember(state) {
        listOf(
            state.owner,
            state.repo,
            state.branch,
            state.workspaceMode.name,
            state.updateBranch,
            state.postsBasePath,
            state.pagesBasePath,
            state.configPath,
            state.personalAccessToken,
            state.defaultAuthor,
            state.defaultLicenseName,
        ).joinToString("|")
    }
    var owner by rememberSaveable(formKey) { mutableStateOf(state.owner) }
    var repo by rememberSaveable(formKey) { mutableStateOf(state.repo) }
    var branch by rememberSaveable(formKey) { mutableStateOf(state.branch) }
    var workspaceMode by rememberSaveable(formKey) { mutableStateOf(state.workspaceMode) }
    var updateBranch by rememberSaveable(formKey) { mutableStateOf(state.updateBranch) }
    var postsBasePath by rememberSaveable(formKey) { mutableStateOf(state.postsBasePath) }
    var pagesBasePath by rememberSaveable(formKey) { mutableStateOf(state.pagesBasePath) }
    var configPath by rememberSaveable(formKey) { mutableStateOf(state.configPath) }
    var personalAccessToken by rememberSaveable(formKey) { mutableStateOf(state.personalAccessToken) }
    var defaultAuthor by rememberSaveable(formKey) { mutableStateOf(state.defaultAuthor) }
    var defaultLicenseName by rememberSaveable(formKey) { mutableStateOf(state.defaultLicenseName) }
    var patGuideExpanded by rememberSaveable { mutableStateOf(false) }

    PrimaryScreenScaffold(
        title = "GitHub 与写作设置",
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("这里负责 GitHub 仓库连接、更新分支策略，以及文章编辑时使用的默认参数。")
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { uriHandler.openUri(GITHUB_MIZUKI_REPO_URL) }) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                Text("打开 Mizuki 仓库", modifier = Modifier.padding(start = 6.dp))
                            }
                            TextButton(onClick = { uriHandler.openUri(MIZUKI_DOCS_URL) }) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                Text("查看 Mizuki 文档", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
            item {
                SettingField(
                    label = "仓库所有者",
                    value = owner,
                    onValueChange = { owner = it },
                    hint = "填写 GitHub 用户名或组织名，例如 matsuzaka-yuki。",
                )
            }
            item {
                SettingField(
                    label = "仓库名称",
                    value = repo,
                    onValueChange = { repo = it },
                    hint = "填写博客仓库名，不需要带 https://github.com。",
                )
            }
            item {
                SettingField(
                    label = "分支名",
                    value = branch,
                    onValueChange = { branch = it },
                    hint = "按仓库真实主分支填写。Mizuki 仓库常见是 master。",
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("更新策略")
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = workspaceMode == WorkspaceMode.DirectCommit,
                                onClick = { workspaceMode = WorkspaceMode.DirectCommit },
                                label = { Text("直接提交") },
                            )
                            FilterChip(
                                selected = workspaceMode == WorkspaceMode.WorkingBranch,
                                onClick = { workspaceMode = WorkspaceMode.WorkingBranch },
                                label = { Text("工作分支") },
                            )
                        }
                        Text(
                            if (workspaceMode == WorkspaceMode.WorkingBranch) {
                                "远程改动将先提交到更新分支，再由你在 GitHub 上合并。"
                            } else {
                                "远程改动将直接写入目标博客分支。"
                            },
                        )
                    }
                }
            }
            item {
                SettingField(
                    label = "更新分支名",
                    value = updateBranch,
                    onValueChange = { updateBranch = it },
                    hint = "仅工作分支模式使用，例如 tsuki/update/blog-console-suite。",
                )
            }
            item {
                SettingField(
                    label = "文章目录",
                    value = postsBasePath,
                    onValueChange = { postsBasePath = it },
                    hint = "Mizuki 默认是 src/content/posts。",
                )
            }
            item {
                SettingField(
                    label = "页面目录",
                    value = pagesBasePath,
                    onValueChange = { pagesBasePath = it },
                    hint = "远程页面目录，默认指向 src/content/spec。",
                )
            }
            item {
                SettingField(
                    label = "配置文件路径",
                    value = configPath,
                    onValueChange = { configPath = it },
                    hint = "Mizuki 主配置默认在 src/config.ts。",
                )
            }
            item {
                SettingField(
                    label = "默认作者",
                    value = defaultAuthor,
                    onValueChange = { defaultAuthor = it },
                    hint = "文章未单独填写作者时，使用这里的默认值。",
                )
            }
            item {
                SettingField(
                    label = "默认许可名",
                    value = defaultLicenseName,
                    onValueChange = { defaultLicenseName = it },
                    hint = "例如 Unlicensed、MIT。",
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("GitHub 细粒度令牌（PAT）")
                        SettingField(
                            label = "GitHub 令牌",
                            value = personalAccessToken,
                            onValueChange = { personalAccessToken = it },
                            hint = "把 GitHub 生成的令牌完整粘贴到这里。",
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        FilledTonalButton(onClick = { patGuideExpanded = !patGuideExpanded }) {
                            Text(if (patGuideExpanded) "收起创建教程" else "展开创建教程")
                        }
                        if (patGuideExpanded) {
                            Text("1. 打开 GitHub 的细粒度令牌创建页。")
                            Text("2. Resource owner 选择你的仓库所属账号或组织。")
                            Text("3. Repository access 选择 Only select repositories，并勾选博客仓库。")
                            Text("4. 在 Repository permissions 中至少开启 Contents = Read and write。")
                            Text("5. 若仓库属于组织，创建后还要在组织里批准该 token。")
                        }
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { uriHandler.openUri(GITHUB_FINE_GRAINED_TOKEN_URL) }) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                Text("前往创建令牌", modifier = Modifier.padding(start = 6.dp))
                            }
                            TextButton(onClick = { uriHandler.openUri(GITHUB_PAT_DOCS_URL) }) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                Text("查看官方教程", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        viewModel.save(
                            state.copy(
                                owner = owner,
                                repo = repo,
                                branch = branch,
                                workspaceMode = workspaceMode,
                                updateBranch = updateBranch,
                                postsBasePath = postsBasePath,
                                pagesBasePath = pagesBasePath,
                                configPath = configPath,
                                personalAccessToken = personalAccessToken,
                                defaultAuthor = defaultAuthor,
                                defaultLicenseName = defaultLicenseName,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("保存设置")
                }
            }
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text(hint) },
        visualTransformation = visualTransformation,
        singleLine = true,
    )
}
