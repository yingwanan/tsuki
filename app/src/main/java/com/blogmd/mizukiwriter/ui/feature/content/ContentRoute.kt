package com.blogmd.mizukiwriter.ui.feature.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.data.github.RemoteContentItem
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.CompactTopBarTextAction
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import com.blogmd.mizukiwriter.ui.theme.SwipeCardMaskDark
import com.blogmd.mizukiwriter.ui.theme.SwipeCardMaskLight

private enum class ContentSourceTab { Local, Remote }
private enum class RemoteContentTab { Published, Draft }
private val ArticleCardShape = RoundedCornerShape(24.dp)

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
            assetStorage = container.assetStorage,
            gitHubPublisher = container.gitHubPublisher,
            workspaceRepository = container.gitHubWorkspaceRepository,
        )
)
    val localDrafts by viewModel.localDrafts.collectAsState()
    val remoteItems by viewModel.remoteItems.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var sourceTab by rememberSaveable { mutableStateOf(ContentSourceTab.Local) }
    var remoteTab by rememberSaveable { mutableStateOf(RemoteContentTab.Published) }
    var pendingLocalDeleteId by remember { mutableStateOf<Long?>(null) }
    var pendingRemoteDelete by remember { mutableStateOf<RemoteContentItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshRemoteContent()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val filteredRemoteItems = remoteItems.filter { item ->
        when (remoteTab) {
            RemoteContentTab.Published -> !item.draft
            RemoteContentTab.Draft -> item.draft
        }
    }

    pendingLocalDeleteId?.let { draftId ->
        DeleteConfirmationDialog(
            title = "删除本地草稿",
            body = "确认删除这个本地草稿吗？删除后只能从远端重新打开已发布内容。",
            onConfirm = {
                viewModel.deleteLocalDraft(draftId)
                pendingLocalDeleteId = null
            },
            onDismiss = { pendingLocalDeleteId = null },
        )
    }

    pendingRemoteDelete?.let { item ->
        DeleteConfirmationDialog(
            title = "删除远端文章",
            body = "确认删除《${item.title ?: item.path.substringAfterLast('/').substringBeforeLast('.')}》吗？这会直接删除 GitHub 上的远端内容。",
            onConfirm = {
                viewModel.deleteRemoteArticle(item.path)
                pendingRemoteDelete = null
            },
            onDismiss = { pendingRemoteDelete = null },
        )
    }

    PrimaryScreenScaffold(
        title = "文章",
        snackbarHostState = snackbarHostState,
        actions = {
            CompactTopBarTextAction(
                label = "刷新",
                icon = Icons.Outlined.CloudSync,
                onClick = { viewModel.refreshRemoteContent() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateDraft) {
                Icon(Icons.Outlined.Add, contentDescription = "新建草稿")
            }
        }
) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SourceSwitchCard(
                    selectedTab = sourceTab,
                    onSelect = { sourceTab = it },
                )
            }

            if (sourceTab == ContentSourceTab.Local) {
                item {
                    SectionCard(
                        title = "本地草稿",
                        subtitle = "继续离线写作，上传成功后会自动清理本地草稿。右滑卡片可快捷删除。",
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
                        SwipeDeleteCard(onDelete = { pendingLocalDeleteId = draft.id }) {
                            ArticleSummaryCard(
                                title = draft.title.ifBlank { "未命名文章" },
                                description = draft.description.ifBlank { "暂无描述" },
                                metaLine = "修改时间：${draft.modifiedAt.toHumanTime()}",
                                statusLine = "本地草稿",
                                onClick = { onOpenDraft(draft.id) },
                            )
                        }
                    }
                }
            } else {
                item {
                    RemoteFilterCard(
                        selectedTab = remoteTab,
                        publishedCount = remoteItems.count { !it.draft },
                        draftCount = remoteItems.count { it.draft },
                        onSelect = { remoteTab = it },
                    )
                }
                item {
                    SectionCard(
                        title = "远端文章",
                        subtitle = "远端内容直接来自 GitHub。点击进入完整编辑器，右滑可快捷删除。",
                    )
                }
                if (filteredRemoteItems.isEmpty()) {
                    item {
                        EmptyCard(
                            title = if (remoteTab == RemoteContentTab.Published) "暂无已发布文章" else "暂无远端草稿",
                            actionText = "重新扫描",
                            onAction = viewModel::refreshRemoteContent,
                        )
                    }
                } else {
                    items(filteredRemoteItems, key = { it.path }) { item ->
                        SwipeDeleteCard(onDelete = { pendingRemoteDelete = item }) {
                            ArticleSummaryCard(
                                title = item.title ?: item.path.substringAfterLast('/').substringBeforeLast('.'),
                                description = item.description ?: "暂无简介",
                                metaLine = "仓库路径：${item.path}",
                                statusLine = if (item.draft) "远端草稿" else "已发布",
                                onClick = {
                                    onOpenRemoteFile(
                                        item.path,
                                        item.title ?: item.path.substringAfterLast('/').substringBeforeLast('.'),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceSwitchCard(
    selectedTab: ContentSourceTab,
    onSelect: (ContentSourceTab) -> Unit,
) {
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("内容来源", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == ContentSourceTab.Local,
                    onClick = { onSelect(ContentSourceTab.Local) },
                    label = { Text("本地草稿") },
                )
                FilterChip(
                    selected = selectedTab == ContentSourceTab.Remote,
                    onClick = { onSelect(ContentSourceTab.Remote) },
                    label = { Text("远端文章") },
                )
            }
        }
    }
}

@Composable
private fun RemoteFilterCard(
    selectedTab: RemoteContentTab,
    publishedCount: Int,
    draftCount: Int,
    onSelect: (RemoteContentTab) -> Unit,
) {
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("远端分类", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == RemoteContentTab.Published,
                    onClick = { onSelect(RemoteContentTab.Published) },
                    label = { Text("已发布 $publishedCount") },
                )
                FilterChip(
                    selected = selectedTab == RemoteContentTab.Draft,
                    onClick = { onSelect(RemoteContentTab.Draft) },
                    label = { Text("远端草稿 $draftCount") },
                )
            }
        }
    }
}

@Composable
private fun SwipeDeleteCard(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val swipeCardMaskColor = if (isSystemInDarkTheme()) SwipeCardMaskDark else SwipeCardMaskLight
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false
        }
)
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (dismissState.progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(ArticleCardShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "删除",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ArticleCardShape)
                .background(swipeCardMaskColor),
        ) {
            content()
        }
    }
}

@Composable
private fun ArticleSummaryCard(
    title: String,
    description: String,
    metaLine: String,
    statusLine: String,
    onClick: () -> Unit,
) {
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = ArticleCardShape,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
            Text(statusLine, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(metaLine, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = { Text(title) },
        text = { Text(body) }
)
}

@Composable
private fun SectionCard(title: String, subtitle: String) {
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
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
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
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
