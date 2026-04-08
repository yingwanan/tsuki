package com.blogmd.mizukiwriter.ui.feature.repositoryfile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blogmd.mizukiwriter.ui.appContainer
import com.blogmd.mizukiwriter.ui.components.PrimaryScreenScaffold
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
        ),
    )
    val state by viewModel.uiState.collectAsState()
    var markdownFrontmatter by remember(state.markdownFrontmatter) { mutableStateOf(state.markdownFrontmatter) }
    var markdownBody by remember(state.markdownBody) { mutableStateOf(state.markdownBody) }
    var bindingValue by remember(state.bindingValue) { mutableStateOf(state.bindingValue) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    PrimaryScreenScaffold(
        title = state.title,
        actions = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            }
            IconButton(
                onClick = {
                    when (state.mode) {
                        RepositoryDocumentMode.Markdown -> viewModel.saveMarkdown(markdownFrontmatter, markdownBody)
                        RepositoryDocumentMode.TypeScriptBinding -> bindingValue?.let(viewModel::saveBinding)
                        RepositoryDocumentMode.Unsupported -> Unit
                    }
                },
            ) {
                Icon(Icons.Outlined.Save, contentDescription = "上传到 GitHub")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(path, style = MaterialTheme.typography.titleSmall)
                        Text("当前分支：${state.branch.ifBlank { "未知" }}", style = MaterialTheme.typography.bodySmall)
                        Text("远程内容已加载到本地编辑态，只有点击右上角上传才会推送 GitHub。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            when (state.mode) {
                RepositoryDocumentMode.Markdown -> {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("Frontmatter", style = MaterialTheme.typography.titleMedium)
                                JsonElementEditor(
                                    label = "frontmatter",
                                    value = markdownFrontmatter,
                                    onChange = { updated ->
                                        markdownFrontmatter = updated as? JsonObject ?: JsonObject(emptyMap())
                                    },
                                )
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("Markdown 正文", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = markdownBody,
                                    onValueChange = { markdownBody = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 12,
                                )
                            }
                        }
                    }
                }

                RepositoryDocumentMode.TypeScriptBinding -> {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("结构化配置", style = MaterialTheme.typography.titleMedium)
                                bindingValue?.let { currentValue ->
                                    JsonElementEditor(
                                        label = bindingName ?: "value",
                                        value = currentValue,
                                        onChange = { bindingValue = it },
                                    )
                                }
                            }
                        }
                    }
                }

                RepositoryDocumentMode.Unsupported -> {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
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
                    Card(modifier = Modifier.fillMaxWidth()) {
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
    onChange: (JsonElement) -> Unit,
) {
    when (value) {
        is JsonObject -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                value.entries.forEach { (key, child) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            JsonElementEditor(
                                label = key,
                                value = child,
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
                            val template = value.firstOrNull()
                            onChange(JsonArray(value + template.blankLike()))
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "新增")
                    }
                }
                value.forEachIndexed { index, child ->
                    Card(modifier = Modifier.fillMaxWidth()) {
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
        LinkedHashMap(entries.associate { (key, value) -> key to value.blankLike() }),
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
