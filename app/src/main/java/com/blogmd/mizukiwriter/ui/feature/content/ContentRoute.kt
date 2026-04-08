package com.blogmd.mizukiwriter.ui.feature.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ContentRoute(
    onCreateDraft: () -> Unit,
    onOpenDraft: (Long) -> Unit,
    onOpenRemoteFile: (String, String) -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: ContentViewModel = viewModel(
        factory = ContentViewModel.factory(
            draftRepository = container.draftRepository,
            settingsRepository = container.settingsRepository,
            workspaceRepository = container.gitHubWorkspaceRepository,
        ),
    )
    val localDrafts by viewModel.localDrafts.collectAsState()
    val remoteItems by viewModel.remoteItems.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refreshRemoteContent()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    PrimaryScreenScaffold(
        title = "文章",
        snackbarHostState = snackbarHostState,
        actions = {
            TextButton(onClick = { viewModel.refreshRemoteContent() }) {
                Icon(Icons.Outlined.CloudSync, contentDescription = null)
                Text("刷新", modifier = Modifier.padding(start = 4.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateDraft) {
                Icon(Icons.Outlined.Add, contentDescription = "新建草稿")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
                item {
                    SectionCard(
                        title = "本地草稿",
                        subtitle = "继续使用本地写作流，点击上传时才推送到仓库。",
                    )
                }
                if (localDrafts.isEmpty()) {
                    item {
                        EmptyCard(
                            title = "还没有本地草稿",
                            actionText = "新建草稿",
                            onAction = onCreateDraft,
                        )
                    }
                } else {
                    items(localDrafts, key = { it.id }) { draft ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenDraft(draft.id) },
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(draft.title.ifBlank { "未命名文章" }, style = MaterialTheme.typography.titleMedium)
                                Text(draft.description.ifBlank { "暂无描述" }, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "修改时间：${draft.modifiedAt.toHumanTime()}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                item {
                    SectionCard(
                        title = "仓库远程文章",
                        subtitle = "远程正式内容直接来自仓库，点击后进入结构化远程编辑页。",
                    )
                }
                if (remoteItems.isEmpty()) {
                    item {
                        EmptyCard(
                            title = "暂无远程内容",
                            actionText = "重新扫描",
                            onAction = viewModel::refreshRemoteContent,
                        )
                    }
                } else {
                    items(remoteItems, key = { it.path }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        item.title ?: item.path.substringAfterLast('/').substringBeforeLast('.'),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        item.description ?: "暂无简介",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        "仓库路径：${item.path}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                TextButton(onClick = {
                                    onOpenRemoteFile(
                                        item.path,
                                        item.title ?: item.path.substringAfterLast('/').substringBeforeLast('.'),
                                    )
                                }) {
                                    Icon(Icons.Outlined.EditNote, contentDescription = null)
                                    Text("打开", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyCard(
    title: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

private fun Long.toHumanTime(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(this))
