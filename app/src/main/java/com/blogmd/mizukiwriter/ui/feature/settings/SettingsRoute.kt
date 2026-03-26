package com.blogmd.mizukiwriter.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenHeader
import com.blogmd.mizukiwriter.ui.appContainer

private const val GITHUB_FINE_GRAINED_TOKEN_URL = "https://github.com/settings/personal-access-tokens/new"
private const val GITHUB_PAT_DOCS_URL = "https://docs.github.com/zh/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens"
private const val GITHUB_MIZUKI_REPO_URL = "https://github.com/matsuzaka-yuki/Mizuki"

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
            state.postsBasePath,
            state.personalAccessToken,
            state.defaultAuthor,
            state.defaultLicenseName,
        ).joinToString("|")
    }
    var owner by rememberSaveable(formKey) { mutableStateOf(state.owner) }
    var repo by rememberSaveable(formKey) { mutableStateOf(state.repo) }
    var branch by rememberSaveable(formKey) { mutableStateOf(state.branch) }
    var postsBasePath by rememberSaveable(formKey) { mutableStateOf(state.postsBasePath) }
    var personalAccessToken by rememberSaveable(formKey) { mutableStateOf(state.personalAccessToken) }
    var defaultAuthor by rememberSaveable(formKey) { mutableStateOf(state.defaultAuthor) }
    var defaultLicenseName by rememberSaveable(formKey) { mutableStateOf(state.defaultLicenseName) }
    var patGuideExpanded by rememberSaveable { mutableStateOf(false) }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryScreenHeader(title = "GitHub 设置")
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 4.dp,
                    end = 16.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("填写 GitHub 仓库信息后，就能直接从应用发布文章。", style = MaterialTheme.typography.titleMedium)
                        Text("默认作者和默认许可名会作为文章预设值。", style = MaterialTheme.typography.bodySmall)
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
                    hint = "按仓库真实主分支填写。Mizuki 仓库常见是 master，不要默认写成 main。",
                )
            }
            item {
                SettingField(
                    label = "文章目录",
                    value = postsBasePath,
                    onValueChange = { postsBasePath = it },
                    hint = "Mizuki 默认是 src/content/posts，通常不需要修改。",
                )
            }
            item {
                SettingField(
                    label = "默认作者",
                    value = defaultAuthor,
                    onValueChange = { defaultAuthor = it },
                    hint = "留空也可以；若文章里单独填写作者，会覆盖这里的默认值。",
                )
            }
            item {
                SettingField(
                    label = "默认许可名",
                    value = defaultLicenseName,
                    onValueChange = { defaultLicenseName = it },
                    hint = "例如 Unlicensed、MIT。若文章里单独填写许可名，会覆盖这里的默认值。",
                )
            }
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("细粒度令牌（PAT）", style = MaterialTheme.typography.titleMedium)
                        Text("这是发布文章到 GitHub 必填的授权令牌。", style = MaterialTheme.typography.bodySmall)
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
                            Text("1. 打开 GitHub 的细粒度令牌创建页。", style = MaterialTheme.typography.bodyMedium)
                            Text("2. Resource owner 选择你的仓库所属账号或组织；如果博客仓库在组织下，这里必须选那个组织。", style = MaterialTheme.typography.bodyMedium)
                            Text("3. Repository access 选择 Only select repositories，并勾选你的博客仓库。", style = MaterialTheme.typography.bodyMedium)
                            Text("4. 在 Permissions 页面继续向下滑，找到 Repository permissions，不要停在上面的 Account permissions / User permissions。", style = MaterialTheme.typography.bodyMedium)
                            Text("5. 本应用只需要 1 项仓库权限：Repository permissions -> Contents = Read and write。", style = MaterialTheme.typography.bodyMedium)
                            Text("6. Metadata 一般会自动带只读权限，不需要你手动修改；Workflows、Actions、Administration 这些通常都不用开。", style = MaterialTheme.typography.bodyMedium)
                            Text("7. 如果你现在看到的是 Codespaces user secrets、Copilot Chat、Copilot Editor Context、Copilot Requests 这些选项，说明你还停在 User permissions，不是仓库权限区域。继续往下滑，或者先检查 Resource owner / Repository access 有没有选对。", style = MaterialTheme.typography.bodyMedium)
                            Text("8. 如果仓库属于组织，创建后还要在 GitHub 里对该组织批准这个 token；未批准时会报权限不足。", style = MaterialTheme.typography.bodyMedium)
                            Text("9. 创建后复制令牌，回到这里粘贴并保存。", style = MaterialTheme.typography.bodyMedium)
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
                Card {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("快捷入口", style = MaterialTheme.typography.titleLarge)
                        Text("如果你要核对 Mizuki 仓库结构，可以直接打开公开仓库。", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { uriHandler.openUri(GITHUB_MIZUKI_REPO_URL) }) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                            Text("打开 Mizuki 仓库", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        viewModel.save(
                            GitHubSettings(
                                owner = owner,
                                repo = repo,
                                branch = branch,
                                postsBasePath = postsBasePath,
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
