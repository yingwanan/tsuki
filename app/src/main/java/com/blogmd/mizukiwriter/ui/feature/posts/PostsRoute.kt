package com.blogmd.mizukiwriter.ui.feature.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.model.PublishState
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsRoute(
    onCreateDraft: () -> Unit,
    onOpenDraft: (Long) -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: PostsViewModel = viewModel(
        factory = PostsViewModel.factory(
            draftRepository = container.draftRepository,
            settingsRepository = container.settingsRepository,
            assetStorage = container.assetStorage,
            gitHubPublisher = container.gitHubPublisher,
        )
)
    val drafts by viewModel.drafts.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<DraftPost?>(null) }
    var deleteRemote by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    pendingDelete?.let { draft ->
        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
                deleteRemote = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDraft(draft, deleteRemote)
                        pendingDelete = null
                        deleteRemote = false
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        deleteRemote = false
                    },
                ) {
                    Text("取消")
                }
            },
            title = { Text("删除文章") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("确认删除《${draft.title.ifBlank { "未命名文章" }}》吗？")
                    if (draft.slug.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(
                                checked = deleteRemote,
                                onCheckedChange = { deleteRemote = it },
                                colors = CheckboxDefaults.colors(),
                            )
                            Text("同时删除 GitHub 远程文章")
                        }
                    }
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onCreateDraft()
                },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "新建文章")
            }
        }
) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            PrimaryScreenHeader(
                title = "文章",
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 4.dp,
                    end = 16.dp,
                    bottom = 92.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("本地写作，发布时再推送到 GitHub。", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "草稿先保存在手机里，发布成功后由博客构建平台自动重建。",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                if (drafts.isEmpty()) {
                    item {
                        Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("还没有草稿", style = MaterialTheme.typography.titleMedium)
                                Text("点击右下角开始写第一篇文章。", style = MaterialTheme.typography.bodySmall)
                                Button(onClick = onCreateDraft) {
                                    Text("新建文章")
                                }
                            }
                        }
                    }
                } else {
                    items(drafts, key = { it.id }) { draft ->
                        DraftCard(
                            draft = draft,
                            onOpenDraft = onOpenDraft,
                            onDeleteDraft = {
                                pendingDelete = draft
                                deleteRemote = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftCard(
    draft: DraftPost,
    onOpenDraft: (Long) -> Unit,
    onDeleteDraft: () -> Unit,
) {
    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenDraft(draft.id) }
) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        draft.title.ifBlank { "未命名文章" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        draft.description.ifBlank { "还没有填写文章描述" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onDeleteDraft,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "删除文章",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("状态：${publishLabel(draft)}", style = MaterialTheme.typography.labelMedium)
                Text("更新于 ${formatTime(draft.modifiedAt)}", style = MaterialTheme.typography.labelMedium)
            }
            if (draft.tags.isNotEmpty()) {
                Text(
                    "标签：${draft.tags.joinToString(" · ")}",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (draft.lastPublishError.isNotBlank()) {
                Text(
                    "错误：${draft.lastPublishError}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun publishLabel(draft: DraftPost): String = when (draft.publishState) {
    PublishState.LocalOnly -> "仅本地"
    PublishState.Syncing -> "发布中"
    PublishState.Synced -> "已发布"
    PublishState.Failed -> "发布失败"
}

private fun formatTime(value: Long): String =
    POSTS_TIME_FORMATTER.format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))

private val POSTS_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
