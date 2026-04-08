package com.blogmd.mizukiwriter.ui.feature.repositoryfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import com.blogmd.mizukiwriter.domain.MarkdownFrontmatterEditor
import com.blogmd.mizukiwriter.domain.TypeScriptModuleEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

enum class RepositoryDocumentMode {
    Markdown,
    TypeScriptBinding,
    Unsupported,
}

data class RepositoryFileUiState(
    val title: String = "",
    val path: String = "",
    val sha: String = "",
    val branch: String = "",
    val source: String = "",
    val bindingName: String? = null,
    val mode: RepositoryDocumentMode = RepositoryDocumentMode.Unsupported,
    val markdownFrontmatter: JsonObject = JsonObject(emptyMap()),
    val markdownBody: String = "",
    val bindingValue: JsonElement? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
)

class RepositoryFileViewModel(
    private val path: String,
    private val title: String,
    private val bindingName: String?,
    private val settingsRepository: SettingsRepositoryContract,
    private val workspaceRepository: GitHubWorkspaceRepositoryContract,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        RepositoryFileUiState(
            title = title.ifBlank { path.substringAfterLast('/') },
            path = path,
            bindingName = bindingName,
        ),
    )
    val uiState: StateFlow<RepositoryFileUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            runCatching {
                val settings = settingsRepository.settings.first()
                val document = workspaceRepository.loadFile(settings, path)
                when {
                    bindingName != null && path.endsWith(".ts") -> {
                        val binding = TypeScriptModuleEditor.parseBinding(document.content, bindingName)
                        _uiState.value = RepositoryFileUiState(
                            title = _uiState.value.title,
                            path = document.path,
                            sha = document.sha,
                            branch = document.branch,
                            source = document.content,
                            bindingName = bindingName,
                            mode = RepositoryDocumentMode.TypeScriptBinding,
                            bindingValue = binding.value,
                            isLoading = false,
                        )
                    }

                    path.endsWith(".md") -> {
                        val markdown = MarkdownFrontmatterEditor.parse(document.content)
                        _uiState.value = RepositoryFileUiState(
                            title = _uiState.value.title,
                            path = document.path,
                            sha = document.sha,
                            branch = document.branch,
                            source = document.content,
                            mode = RepositoryDocumentMode.Markdown,
                            markdownFrontmatter = markdown.frontmatter,
                            markdownBody = markdown.body,
                            isLoading = false,
                        )
                    }

                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            source = document.content,
                            branch = document.branch,
                            sha = document.sha,
                            mode = RepositoryDocumentMode.Unsupported,
                            message = "当前文件暂不支持结构化编辑：$path",
                        )
                    }
                }
            }.onFailure { error ->
                val targetName = bindingName ?: path
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = error.message ?: "解析远程内容失败：$targetName",
                )
            }
        }
    }

    fun saveMarkdown(frontmatter: JsonObject, body: String) {
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.settings.first()
                val updated = MarkdownFrontmatterEditor.update(frontmatter = frontmatter, body = body)
                workspaceRepository.saveFile(
                    settings = settings,
                    path = path,
                    content = updated,
                    commitMessage = "chore(content): update $path",
                )
                _uiState.value = _uiState.value.copy(
                    source = updated,
                    markdownFrontmatter = frontmatter,
                    markdownBody = body,
                    message = "GitHub 文件已上传",
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(message = error.message ?: "上传失败")
            }
        }
    }

    fun saveBinding(value: JsonElement) {
        val currentSource = _uiState.value.source
        val currentBinding = bindingName ?: return
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.settings.first()
                val updated = TypeScriptModuleEditor.updateBinding(
                    source = currentSource,
                    bindingName = currentBinding,
                ) { value }
                workspaceRepository.saveFile(
                    settings = settings,
                    path = path,
                    content = updated,
                    commitMessage = "chore(content): update $path",
                )
                _uiState.value = _uiState.value.copy(
                    source = updated,
                    bindingValue = value,
                    message = "GitHub 文件已上传",
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(message = error.message ?: "上传失败")
            }
        }
    }

    companion object {
        fun factory(
            path: String,
            title: String,
            bindingName: String?,
            settingsRepository: SettingsRepositoryContract,
            workspaceRepository: GitHubWorkspaceRepositoryContract,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RepositoryFileViewModel(
                    path = path,
                    title = title,
                    bindingName = bindingName,
                    settingsRepository = settingsRepository,
                    workspaceRepository = workspaceRepository,
                ) as T
            }
        }
    }
}
