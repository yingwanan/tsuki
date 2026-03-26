package com.blogmd.mizukiwriter.ui.feature.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blogmd.mizukiwriter.data.github.GitHubDeleteResult
import com.blogmd.mizukiwriter.data.github.GitHubPublisherContract
import com.blogmd.mizukiwriter.data.media.AssetStorageContract
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.model.PublishState
import com.blogmd.mizukiwriter.data.repository.DraftRepositoryContract
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostsViewModel(
    private val draftRepository: DraftRepositoryContract,
    private val settingsRepository: SettingsRepositoryContract,
    private val assetStorage: AssetStorageContract,
    private val gitHubPublisher: GitHubPublisherContract,
) : ViewModel() {
    val drafts = draftRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun createDraft(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(draftRepository.createBlankDraft())
        }
    }

    fun deleteDraft(draft: DraftPost, deleteRemote: Boolean) {
        viewModelScope.launch {
            val remoteResult = if (deleteRemote && draft.publishState == PublishState.Synced && draft.slug.isNotBlank()) {
                gitHubPublisher.deleteRemoteArticle(draft, settingsRepository.settings.first())
            } else {
                null
            }

            draftRepository.delete(draft.id)
            assetStorage.deleteDraftAssets(draft.id)
            _message.value = when (remoteResult) {
                is GitHubDeleteResult.Success -> "文章已删除，并已移除 GitHub 远程文章"
                is GitHubDeleteResult.Failure -> "本地草稿已删除，GitHub 删除失败：${remoteResult.message}"
                null -> "本地草稿已删除"
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
            assetStorage: AssetStorageContract,
            gitHubPublisher: GitHubPublisherContract,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PostsViewModel(
                    draftRepository = draftRepository,
                    settingsRepository = settingsRepository,
                    assetStorage = assetStorage,
                    gitHubPublisher = gitHubPublisher,
                ) as T
            }
        }
    }
}
