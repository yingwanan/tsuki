package com.blogmd.mizukiwriter.ui.feature.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
import com.blogmd.mizukiwriter.data.github.RemoteContentItem
import com.blogmd.mizukiwriter.data.github.RemoteContentType
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.repository.DraftRepositoryContract
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ContentViewModel(
    draftRepository: DraftRepositoryContract,
    private val settingsRepository: SettingsRepositoryContract,
    private val workspaceRepository: GitHubWorkspaceRepositoryContract,
) : ViewModel() {
    val localDrafts: StateFlow<List<DraftPost>> = draftRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _remoteItems = MutableStateFlow<List<RemoteContentItem>>(emptyList())
    val remoteItems: StateFlow<List<RemoteContentItem>> = _remoteItems.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun refreshRemoteContent() {
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.settings.first()
                _remoteItems.value = workspaceRepository.listRemoteContent(settings)
                    .filter { it.type == RemoteContentType.Post }
            }.onFailure { error ->
                _message.value = error.message ?: "加载远程内容失败"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    companion object {
        fun factory(
            draftRepository: DraftRepositoryContract,
            settingsRepository: SettingsRepositoryContract,
            workspaceRepository: GitHubWorkspaceRepositoryContract,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContentViewModel(draftRepository, settingsRepository, workspaceRepository) as T
            }
        }
    }
}
