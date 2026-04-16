package com.blogmd.mizukiwriter.ui.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.domain.MarkdownAction
import com.blogmd.mizukiwriter.domain.MarkdownEditorEngine
import com.blogmd.mizukiwriter.ui.appContainer
import kotlinx.coroutines.delay
import androidx.compose.material3.CardDefaults
import com.blogmd.mizukiwriter.ui.components.MarkdownEditorMode
import com.blogmd.mizukiwriter.ui.components.MarkdownEditorToolbar
import com.blogmd.mizukiwriter.ui.components.MarkdownEditorWorkspace

private enum class PickerTarget { Cover, Inline }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorRoute(
    draftId: Long,
    remoteArticlePath: String? = null,
    remoteArticleTitle: String? = null,
    onBack: () -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: EditorViewModel = viewModel(
        key = "editor-$draftId-${remoteArticlePath.orEmpty()}",
        factory = EditorViewModel.factory(
            draftId = draftId,
            remoteArticlePath = remoteArticlePath,
            remoteArticleTitle = remoteArticleTitle,
            draftRepository = container.draftRepository,
            settingsRepository = container.settingsRepository,
            assetStorage = container.assetStorage,
            gitHubPublisher = container.gitHubPublisher,
            workspaceRepository = container.gitHubWorkspaceRepository,
        ),
    )
    val isRemoteSession = viewModel.isRemoteSession
    val draft by viewModel.draft.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val conflictPath by viewModel.conflictPath.collectAsState()
    val draftDeleted by viewModel.draftDeleted.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(draftDeleted) {
        if (draftDeleted) onBack()
    }

    val currentDraft = draft
    if (currentDraft == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("正在准备草稿…")
        }
        return
    }

    var previewMode by remember(currentDraft.id) { mutableStateOf(MarkdownEditorMode.Edit) }
    var metadataExpanded by remember(currentDraft.id) {
        mutableStateOf(currentDraft.title.isBlank() && currentDraft.description.isBlank())
    }
    var advancedExpanded by remember(currentDraft.id) { mutableStateOf(false) }
    var pendingPickerTarget by remember { mutableStateOf(PickerTarget.Inline) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteRemote by remember(currentDraft.id, isRemoteSession) { mutableStateOf(false) }
    var bodyValue by remember(currentDraft.id) { mutableStateOf(TextFieldValue(currentDraft.body)) }
    var previewContent by remember(currentDraft.id) {
        mutableStateOf(previewMarkdown(currentDraft, settings.defaultAuthor))
    }

    LaunchedEffect(currentDraft.body) {
        if (currentDraft.body != bodyValue.text) {
            bodyValue = syncExternalTextFieldValue(
                current = bodyValue,
                externalText = currentDraft.body,
            )
        }
    }

    LaunchedEffect(currentDraft.id, previewMode) {
        if (previewMode != MarkdownEditorMode.Edit) {
            previewContent = previewMarkdown(currentDraft, settings.defaultAuthor)
        }
    }

    LaunchedEffect(
        previewMode,
        currentDraft.title,
        currentDraft.description,
        currentDraft.author,
        currentDraft.body,
        settings.defaultAuthor
) {
        if (previewMode == MarkdownEditorMode.Edit) return@LaunchedEffect
        delay(PREVIEW_UPDATE_DELAY_MS)
        previewContent = previewMarkdown(currentDraft, settings.defaultAuthor)
    }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        when (pendingPickerTarget) {
            PickerTarget.Cover -> viewModel.importAsset(context.contentResolver, uri, asCover = true)
            PickerTarget.Inline -> {
                val relativePath = container.assetStorage.importAsset(
                    draftId = currentDraft.id,
                    sourceUri = uri,
                    resolver = context.contentResolver,
                    preferredBaseName = null,
                )
                val editResult = MarkdownEditorEngine.apply(
                    text = bodyValue.text,
                    selectionStart = bodyValue.selection.start,
                    selectionEnd = bodyValue.selection.end,
                    action = MarkdownAction.Image(relativePath = relativePath, alt = "image"),
                )
                bodyValue = TextFieldValue(
                    text = editResult.text,
                    selection = TextRange(editResult.selectionStart, editResult.selectionEnd),
                )
                viewModel.updateDraft { it.copy(body = editResult.text) }
            }
        }
    }

    if (conflictPath != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConflict,
            confirmButton = {
                TextButton(onClick = { viewModel.publish(overwrite = true) }) {
                    Text("覆盖发布")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConflict) {
                    Text("取消")
                }
            },
            title = { Text("发现同名文章") },
            text = { Text("远程路径已存在：$conflictPath") },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = {
                confirmDelete = false
                deleteRemote = false
            },
            confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteDraft(deleteRemote = deleteRemote)
                            confirmDelete = false
                        },
                    ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        deleteRemote = false
                    },
                ) {
                    Text("取消")
                }
            },
            title = { Text("删除文章") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("确认删除《${currentDraft.title.ifBlank { "未命名文章" }}》吗？")
                    if (!isRemoteSession && currentDraft.slug.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = deleteRemote, onCheckedChange = { deleteRemote = it })
                            Text("同时删除 GitHub 远程文章")
                        }
                    } else if (isRemoteSession) {
                        Text("这会直接删除 GitHub 上的远端文章。")
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentDraft.title.ifBlank { "写作" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.publish() }) {
                        Icon(Icons.Outlined.Publish, contentDescription = "发布")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除文章")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (previewMode != MarkdownEditorMode.Preview) {
                Surface(shadowElevation = 8.dp) {
                    MarkdownEditorToolbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding(),
                        onAction = { action ->
                            val editResult = MarkdownEditorEngine.apply(
                                text = bodyValue.text,
                                selectionStart = bodyValue.selection.start,
                                selectionEnd = bodyValue.selection.end,
                                action = action,
                            )
                            bodyValue = TextFieldValue(
                                text = editResult.text,
                                selection = TextRange(editResult.selectionStart, editResult.selectionEnd),
                            )
                            viewModel.updateDraft { it.copy(body = editResult.text) }
                        },
                        onPickImage = {
                            pendingPickerTarget = PickerTarget.Inline
                            pickerLauncher.launch(arrayOf("image/*"))
                        },
                    )
                }
            }
        }
) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetadataPanel(
                draft = currentDraft,
                expanded = metadataExpanded,
                advancedExpanded = advancedExpanded,
                onToggle = { metadataExpanded = !metadataExpanded },
                onToggleAdvanced = { advancedExpanded = !advancedExpanded },
                onUpdateDraft = viewModel::updateDraft,
                onFillDates = viewModel::fillPrimaryDatesToday,
                onFillUpdated = viewModel::fillUpdatedToday,
                onPickCover = {
                    pendingPickerTarget = PickerTarget.Cover
                    pickerLauncher.launch(arrayOf("image/*"))
                },
            )

            MarkdownEditorWorkspace(
                mode = previewMode,
                onModeSelected = { previewMode = it },
                bodyValue = bodyValue,
                onBodyValueChange = {
                    bodyValue = it
                    viewModel.updateDraft { draftState -> draftState.copy(body = it.text) }
                },
                previewMarkdown = previewContent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataPanel(
    draft: DraftPost,
    expanded: Boolean,
    advancedExpanded: Boolean,
    onToggle: () -> Unit,
    onToggleAdvanced: () -> Unit,
    onUpdateDraft: ((DraftPost) -> DraftPost) -> Unit,
    onFillDates: () -> Unit,
    onFillUpdated: () -> Unit,
    onPickCover: () -> Unit,
) {
    val missingCount = listOf(draft.title, draft.description).count { it.isBlank() }

    Card {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("元数据", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (missingCount == 0) "必填项已完成" else "还有 $missingCount 个必填项未填写",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (missingCount == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onToggle) {
                    Text(if (expanded) "收起" else "展开")
                }
            }

            AnimatedVisibility(expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetadataTextField(
                        label = "标题 *",
                        value = draft.title,
                        onValueChange = { value -> onUpdateDraft { current -> current.copy(title = value) } },
                    )
                    MetadataTextField(
                        label = "文章描述 *",
                        value = draft.description,
                        onValueChange = { value -> onUpdateDraft { current -> current.copy(description = value) } },
                        minLines = 2,
                        maxLines = 3,
                        singleLine = false,
                    )
                    TwoFieldRow(
                        first = {
                            MetadataTextField(
                                label = "发布时间",
                                value = draft.published,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(published = value) } },
                            )
                        },
                        second = {
                            MetadataTextField(
                                label = "更新时间",
                                value = draft.updated,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(updated = value) } },
                            )
                        },
                    )
                    TwoFieldRow(
                        first = {
                            MetadataTextField(
                                label = "date",
                                value = draft.date,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(date = value) } },
                            )
                        },
                        second = {
                            MetadataTextField(
                                label = "pubDate",
                                value = draft.pubDate,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(pubDate = value) } },
                            )
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = onFillDates,
                            label = { Text("全部填今天") },
                            leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                        )
                        AssistChip(
                            onClick = onFillUpdated,
                            label = { Text("仅更新改动时间") },
                            leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                        )
                        AssistChip(
                            onClick = onPickCover,
                            label = { Text("选择封面") },
                            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                        )
                    }
                    if (draft.image.isNotBlank()) {
                        Text("封面路径：${draft.image}", style = MaterialTheme.typography.bodySmall)
                    }
                    TwoFieldRow(
                        first = {
                            MetadataTextField(
                                label = "分类",
                                value = draft.category,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(category = value) } },
                            )
                        },
                        second = {
                            MetadataTextField(
                                label = "语言",
                                value = draft.lang,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(lang = value) } },
                            )
                        },
                    )
                    TwoFieldRow(
                        first = {
                            MetadataTextField(
                                label = "标签（逗号分隔）",
                                value = draft.tags.joinToString(", "),
                                onValueChange = { value ->
                                    onUpdateDraft { current ->
                                        current.copy(tags = value.split(",").map { it.trim() }.filter { it.isNotBlank() })
                                    }
                                },
                            )
                        },
                        second = {
                            MetadataTextField(
                                label = "别名（逗号分隔）",
                                value = draft.alias.joinToString(", "),
                                onValueChange = { value ->
                                    onUpdateDraft { current ->
                                        current.copy(alias = value.split(",").map { it.trim() }.filter { it.isNotBlank() })
                                    }
                                },
                            )
                        },
                    )
                    TwoFieldRow(
                        first = {
                            MetadataTextField(
                                label = "作者",
                                value = draft.author,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(author = value) } },
                            )
                        },
                        second = {
                            MetadataTextField(
                                label = "许可名",
                                value = draft.licenseName,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(licenseName = value) } },
                            )
                        },
                    )
                    TwoFieldRow(
                        first = {
                            MetadataTextField(
                                label = "来源链接",
                                value = draft.sourceLink,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(sourceLink = value) } },
                            )
                        },
                        second = {
                            MetadataTextField(
                                label = "许可链接",
                                value = draft.licenseUrl,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(licenseUrl = value) } },
                            )
                        },
                    )
                    TwoFieldRow(
                        first = {
                            MetadataTextField(
                                label = "固定链接",
                                value = draft.permalink,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(permalink = value) } },
                            )
                        },
                        second = {
                            MetadataTextField(
                                label = "封面路径",
                                value = draft.image,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(image = value) } },
                            )
                        },
                    )
                    TwoFieldRow(
                        first = {
                            MetadataTextField(
                                label = "优先级",
                                value = draft.priority.takeIf { it > 0 }?.toString().orEmpty(),
                                onValueChange = { value ->
                                    onUpdateDraft { current -> current.copy(priority = value.toIntOrNull() ?: 0) }
                                },
                                keyboardType = KeyboardType.Number,
                            )
                        },
                        second = {
                            MetadataTextField(
                                label = "密码",
                                value = draft.password,
                                onValueChange = { value -> onUpdateDraft { current -> current.copy(password = value) } },
                            )
                        },
                    )
                    MetadataTextField(
                        label = "密码提示",
                        value = draft.passwordHint,
                        onValueChange = { value -> onUpdateDraft { current -> current.copy(passwordHint = value) } },
                    )
                    HorizontalDivider()
                    SwitchRow(
                        firstLabel = "草稿",
                        firstChecked = draft.draft,
                        onFirstChange = { checked -> onUpdateDraft { current -> current.copy(draft = checked) } },
                        secondLabel = "置顶",
                        secondChecked = draft.pinned,
                        onSecondChange = { checked -> onUpdateDraft { current -> current.copy(pinned = checked) } },
                    )
                    SwitchRow(
                        firstLabel = "允许评论",
                        firstChecked = draft.comment,
                        onFirstChange = { checked -> onUpdateDraft { current -> current.copy(comment = checked) } },
                        secondLabel = "加密",
                        secondChecked = draft.encrypted,
                        onSecondChange = { checked -> onUpdateDraft { current -> current.copy(encrypted = checked) } },
                    )
                    TextButton(onClick = onToggleAdvanced) {
                        Text(if (advancedExpanded) "收起高级提示" else "展开高级提示")
                    }
                    AnimatedVisibility(advancedExpanded) {
                        Text(
                            "作者和许可名留空时，会自动回退到设置页中的默认值。date、pubDate、updated 可以按博客需要分别填写，也可以用上方快捷按钮一起填入今天。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = 1,
    singleLine: Boolean = true,
) {
    var textFieldValue by rememberSaveable(label, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value))
    }

    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = syncExternalTextFieldValue(
                current = textFieldValue,
                externalText = value,
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { updated ->
            textFieldValue = updated
            onValueChange(updated.text)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        maxLines = maxLines,
        singleLine = singleLine
)
}

internal fun syncExternalTextFieldValue(
    current: TextFieldValue,
    externalText: String,
): TextFieldValue {
    if (current.text == externalText) return current
    val maxIndex = externalText.length
    return current.copy(
        text = externalText,
        selection = TextRange(
            start = current.selection.start.coerceIn(0, maxIndex),
            end = current.selection.end.coerceIn(0, maxIndex),
        )
)
}

private const val PREVIEW_UPDATE_DELAY_MS = 180L

@Composable
private fun TwoFieldRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
        Box(modifier = Modifier.weight(1f)) { first() }
        Box(modifier = Modifier.weight(1f)) { second() }
    }
}

@Composable
private fun SwitchRow(
    firstLabel: String,
    firstChecked: Boolean,
    onFirstChange: (Boolean) -> Unit,
    secondLabel: String,
    secondChecked: Boolean,
    onSecondChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
        SwitchItem(
            modifier = Modifier.weight(1f),
            label = firstLabel,
            checked = firstChecked,
            onCheckedChange = onFirstChange,
        )
        SwitchItem(
            modifier = Modifier.weight(1f),
            label = secondLabel,
            checked = secondChecked,
            onCheckedChange = onSecondChange,
        )
    }
}

@Composable
private fun SwitchItem(
    modifier: Modifier,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun previewMarkdown(
    draft: DraftPost,
    defaultAuthor: String,
): String = buildString {
    append("# ")
    appendLine(draft.title.ifBlank { "未命名文章" })
    if (draft.description.isNotBlank()) {
        appendLine()
        appendLine("> ${draft.description}")
    }
    val author = draft.author.ifBlank { defaultAuthor }
    if (author.isNotBlank()) {
        appendLine()
        appendLine("_${author}_")
    }
    appendLine()
    append(draft.body.ifBlank { "开始写作后，预览会出现在这里。" })
}
