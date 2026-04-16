package com.blogmd.mizukiwriter.ui.feature.repositoryfile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.domain.MizukiCatalog
import com.blogmd.mizukiwriter.domain.MarkdownEditorEngine
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.CompactTopBarIconButton
import com.blogmd.mizukiwriter.ui.components.MarkdownEditorMode
import com.blogmd.mizukiwriter.ui.components.MarkdownEditorToolbar
import com.blogmd.mizukiwriter.ui.components.MarkdownEditorWorkspace
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold
import com.blogmd.mizukiwriter.ui.feature.editor.syncExternalTextFieldValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults

@Composable
fun RepositoryFileRoute(
    path: String,
    title: String? = null,
    bindingName: String? = null,
    onBack: (() -> Unit)?,
) {
    val container = LocalContext.current.appContainer
    val viewModel: RepositoryFileViewModel = viewModel(
        key = "repository-file-$path-${bindingName.orEmpty()}",
        factory = RepositoryFileViewModel.factory(
            path = path,
            title = title.orEmpty(),
            bindingName = bindingName,
            settingsRepository = container.settingsRepository,
            workspaceRepository = container.gitHubWorkspaceRepository,
        )
)
    val state by viewModel.uiState.collectAsState()
    var markdownFrontmatter by remember(state.markdownFrontmatter) { mutableStateOf(state.markdownFrontmatter) }
    var markdownBody by remember(state.markdownBody) { mutableStateOf(state.markdownBody) }
    var markdownBodyValue by remember(state.sha, state.markdownBody) { mutableStateOf(TextFieldValue(state.markdownBody)) }
    var markdownEditorMode by remember(state.sha, path) { mutableStateOf(MarkdownEditorMode.Edit) }
    var bindingValue by remember(state.bindingValue) { mutableStateOf(state.bindingValue) }
    val labelResolver: (String) -> String = remember(path, bindingName) {
        { key -> MizukiCatalog.resolveFieldLabel(path = path, bindingName = bindingName, key = key) }
    }
    val rootArrayItemTemplate = remember(path, bindingName) {
        MizukiCatalog.createArrayItemTemplate(path = path, bindingName = bindingName)
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(markdownBody) {
        if (markdownBody != markdownBodyValue.text) {
            markdownBodyValue = syncExternalTextFieldValue(
                current = markdownBodyValue,
                externalText = markdownBody,
            )
        }
    }

    PrimaryScreenScaffold(
        title = state.title,
        navigationIcon = {
            if (onBack != null) {
                CompactTopBarIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack,
                )
            }
        },
        actions = {
            CompactTopBarIconButton(
                icon = Icons.Outlined.Save,
                contentDescription = "上传到 GitHub",
                onClick = {
                    when (state.mode) {
                        RepositoryDocumentMode.Markdown -> viewModel.saveMarkdown(markdownFrontmatter, markdownBody)
                        RepositoryDocumentMode.TypeScriptBinding -> bindingValue?.let(viewModel::saveBinding)
                        RepositoryDocumentMode.Unsupported -> Unit
                    }
                },
            )
        },
        bottomBar = {
            if (state.mode == RepositoryDocumentMode.Markdown && markdownEditorMode != MarkdownEditorMode.Preview) {
                Surface(shadowElevation = 8.dp) {
                    MarkdownEditorToolbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding(),
                        onAction = { action ->
                            val editResult = MarkdownEditorEngine.apply(
                                text = markdownBodyValue.text,
                                selectionStart = markdownBodyValue.selection.start,
                                selectionEnd = markdownBodyValue.selection.end,
                                action = action,
                            )
                            markdownBodyValue = TextFieldValue(
                                text = editResult.text,
                                selection = TextRange(editResult.selectionStart, editResult.selectionEnd),
                            )
                            markdownBody = editResult.text
                        },
                    )
                }
            }
        },
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
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("仓库路径：$path", style = MaterialTheme.typography.titleSmall)
                        Text("当前分支：${state.branch.ifBlank { "未知" }}", style = MaterialTheme.typography.bodySmall)
                        Text("远程内容已加载到本地编辑态，只有点击右上角上传才会推送到仓库。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            when (state.mode) {
                RepositoryDocumentMode.Markdown -> {
                    item {
                        Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("页面信息", style = MaterialTheme.typography.titleMedium)
                                JsonElementEditor(
                                    label = MizukiCatalog.rootSectionLabel(path = path, bindingName = bindingName),
                                    value = markdownFrontmatter,
                                    labelResolver = labelResolver,
                                    onChange = { updated ->
                                        markdownFrontmatter = updated as? JsonObject ?: JsonObject(emptyMap())
                                    },
                                )
                            }
                        }
                    }
                    item {
                        Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("Markdown 正文", style = MaterialTheme.typography.titleMedium)
                                MarkdownEditorWorkspace(
                                    mode = markdownEditorMode,
                                    onModeSelected = { markdownEditorMode = it },
                                    bodyValue = markdownBodyValue,
                                    onBodyValueChange = {
                                        markdownBodyValue = it
                                        markdownBody = it.text
                                    },
                                    previewMarkdown = markdownBody.ifBlank { "开始写作后，预览会出现在这里。" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 420.dp),
                                    editPlaceholder = "开始编辑 Markdown…",
                                )
                            }
                        }
                    }
                }

                RepositoryDocumentMode.TypeScriptBinding -> {
                    item {
                        Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("结构化内容", style = MaterialTheme.typography.titleMedium)
                                bindingValue?.let { currentValue ->
                                    JsonElementEditor(
                                        label = MizukiCatalog.rootSectionLabel(path = path, bindingName = bindingName),
                                        value = currentValue,
                                        labelResolver = labelResolver,
                                        rootArrayItemTemplate = rootArrayItemTemplate,
                                        onChange = { bindingValue = it },
                                    )
                                }
                            }
                        }
                    }
                }

                RepositoryDocumentMode.Unsupported -> {
                    item {
                        Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = state.message ?: "当前文件暂不支持结构化编辑。",
                                modifier = Modifier.padding(18.dp),
                            )
                        }
                    }
                }
            }
            state.message?.let { message ->
                item {
                    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JsonElementEditor(
    label: String,
    value: JsonElement,
    labelResolver: (String) -> String,
    rootArrayItemTemplate: JsonElement? = null,
    isRoot: Boolean = true,
    onChange: (JsonElement) -> Unit,
) {
    when (value) {
        is JsonObject -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                value.entries.forEach { (key, child) ->
                    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            JsonElementEditor(
                                label = labelResolver(key),
                                value = child,
                                labelResolver = labelResolver,
                                rootArrayItemTemplate = null,
                                isRoot = false,
                                onChange = { updatedChild ->
                                    onChange(
                                        JsonObject(
                                            LinkedHashMap(value).apply { put(key, updatedChild) },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        is JsonArray -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, style = MaterialTheme.typography.titleSmall)
                    IconButton(
                        onClick = {
                            val template = if (isRoot) {
                                rootArrayItemTemplate ?: value.firstOrNull().blankLike()
                            } else {
                                value.firstOrNull().blankLike()
                            }
                            onChange(JsonArray(value + template))
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "新增")
                    }
                }
                value.forEachIndexed { index, child ->
                    Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("${label} #${index + 1}", style = MaterialTheme.typography.labelLarge)
                                IconButton(
                                    onClick = {
                                        onChange(
                                            JsonArray(
                                                value.filterIndexed { childIndex, _ -> childIndex != index },
                                            ),
                                        )
                                    },
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除")
                                }
                            }
                            JsonElementEditor(
                                label = label,
                                value = child,
                                labelResolver = labelResolver,
                                rootArrayItemTemplate = null,
                                isRoot = false,
                                onChange = { updatedChild ->
                                    onChange(
                                        JsonArray(
                                            value.mapIndexed { childIndex, oldValue ->
                                                if (childIndex == index) updatedChild else oldValue
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        is JsonPrimitive -> {
            if (value.content == "true" || value.content == "false") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = value.content == "true",
                        onCheckedChange = { checked -> onChange(JsonPrimitive(checked)) },
                    )
                }
            } else {
                OutlinedTextField(
                    value = value.content,
                    onValueChange = { updated ->
                        onChange(updated.toJsonPrimitiveLike(value))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(label) },
                    singleLine = !value.content.contains('\n'),
                )
            }
        }

        JsonNull -> {
            OutlinedTextField(
                value = "",
                onValueChange = { onChange(JsonPrimitive(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
            )
        }
    }
}

private fun String.toJsonPrimitiveLike(previous: JsonPrimitive): JsonPrimitive {
    if (previous.isString) return JsonPrimitive(this)
    return when {
        toLongOrNull() != null -> JsonPrimitive(toLong())
        toDoubleOrNull() != null -> JsonPrimitive(toDouble())
        this == "true" || this == "false" -> JsonPrimitive(this == "true")
        else -> JsonPrimitive(this)
    }
}

private fun JsonElement?.blankLike(): JsonElement = when (this) {
    is JsonObject -> JsonObject(
        LinkedHashMap(entries.associate { (key, value) -> key to value.blankLike() })
)
    is JsonArray -> JsonObject(emptyMap())
    is JsonPrimitive -> when {
        content == "true" || content == "false" -> JsonPrimitive(false)
        content.toLongOrNull() != null -> JsonPrimitive(0)
        content.toDoubleOrNull() != null -> JsonPrimitive(0.0)
        else -> JsonPrimitive("")
    }
    JsonNull, null -> JsonPrimitive("")
}
