package com.blogmd.mizukiwriter.ui.feature.editor

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blogmd.mizukiwriter.data.github.GitHubDeleteResult
import com.blogmd.mizukiwriter.data.github.GitHubPublishResult
import com.blogmd.mizukiwriter.data.github.GitHubPublisherContract
import com.blogmd.mizukiwriter.data.media.AssetStorageContract
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.model.PublishState
import com.blogmd.mizukiwriter.data.repository.DraftRepositoryContract
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModel(
    draftId: Long,
    private val draftRepository: DraftRepositoryContract,
    private val settingsRepository: SettingsRepositoryContract,
    private val assetStorage: AssetStorageContract,
    private val gitHubPublisher: GitHubPublisherContract,
) : ViewModel() {
    private val currentDraftId = MutableStateFlow(draftId)
    private val repositoryDraft: StateFlow<DraftPost?> = currentDraftId
        .filter { it > 0L }
        .flatMapLatest { draftRepository.observeById(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val localDraft = MutableStateFlow<DraftPost?>(null)
    private var delayedSaveJob: Job? = null

    val draft: StateFlow<DraftPost?> = combine(repositoryDraft, localDraft, ::mergeDraftForUi)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<GitHubSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, GitHubSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _conflictPath = MutableStateFlow<String?>(null)
    val conflictPath: StateFlow<String?> = _conflictPath.asStateFlow()

    private val _draftDeleted = MutableStateFlow(false)
    val draftDeleted: StateFlow<Boolean> = _draftDeleted.asStateFlow()

    init {
        if (draftId == 0L) {
            viewModelScope.launch {
                currentDraftId.value = draftRepository.createBlankDraft()
            }
        }
    }

    fun updateDraft(transform: (DraftPost) -> DraftPost) {
        val snapshot = draft.value ?: return
        localDraft.value = transform(snapshot)
        scheduleDelayedSave()
    }

    fun fillPrimaryDatesToday() {
        val today = LocalDate.now().toString()
        updateDraft {
            it.copy(
                published = today,
                updated = today,
                date = today,
                pubDate = today,
            )
        }
    }

    fun fillUpdatedToday() {
        updateDraft { it.copy(updated = LocalDate.now().toString()) }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun dismissConflict() {
        _conflictPath.value = null
    }

    fun importAsset(
        resolver: ContentResolver,
        uri: Uri,
        asCover: Boolean,
    ) {
        val snapshot = draft.value ?: return
        viewModelScope.launch {
            val relativePath = assetStorage.importAsset(
                draftId = snapshot.id,
                sourceUri = uri,
                resolver = resolver,
                preferredBaseName = if (asCover) "cover" else null,
            )
            if (asCover) {
                persistDraftNow(snapshot.copy(image = relativePath))
                _message.value = "封面已导入"
            } else {
                _message.value = relativePath
            }
        }
    }

    fun publish(overwrite: Boolean = false) {
        val snapshot = draft.value ?: return
        validateForPublish(snapshot)?.let {
            _message.value = it
            return
        }
        val effectiveOverwrite = overwrite || (
            snapshot.slug.isNotBlank()
            )
        viewModelScope.launch {
            val latest = persistDraftNow(snapshot)
            draftRepository.save(latest.copy(publishState = PublishState.Syncing, lastPublishError = ""))
            when (val result = gitHubPublisher.publish(latest, settings.first(), effectiveOverwrite)) {
                is GitHubPublishResult.Success -> {
                    draftRepository.markPublishSuccess(latest, result.slug)
                    _message.value = "发布成功：${result.slug}"
                    _conflictPath.value = null
                }
                is GitHubPublishResult.Conflict -> {
                    draftRepository.markPublishFailure(latest, "远程已存在同名文章")
                    _conflictPath.value = result.path
                }
                is GitHubPublishResult.Failure -> {
                    draftRepository.markPublishFailure(latest, result.message)
                    _message.value = result.message
                }
            }
        }
    }

    fun deleteDraft(deleteRemote: Boolean) {
        val snapshot = draft.value ?: return
        viewModelScope.launch {
            val settings = settings.first()
            val remoteResult = if (deleteRemote && snapshot.slug.isNotBlank()) {
                gitHubPublisher.deleteRemoteArticle(snapshot, settings)
            } else {
                null
            }

            draftRepository.delete(snapshot.id)
            assetStorage.deleteDraftAssets(snapshot.id)
            _draftDeleted.value = true
            _message.value = when (remoteResult) {
                is GitHubDeleteResult.Success -> "文章已删除，并已移除 GitHub 远程文章"
                is GitHubDeleteResult.Failure -> "本地草稿已删除，GitHub 删除失败：${remoteResult.message}"
                null -> "本地草稿已删除"
            }
        }
    }

    private fun validateForPublish(draft: DraftPost): String? = when {
        draft.title.isBlank() -> "请先填写标题"
        draft.description.isBlank() -> "请先填写文章描述"
        draft.published.isBlank() -> "请先填写发布时间"
        else -> null
    }

    private fun scheduleDelayedSave() {
        delayedSaveJob?.cancel()
        delayedSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            persistDraftNow(draft.value ?: return@launch)
        }
    }

    private suspend fun persistDraftNow(snapshot: DraftPost): DraftPost {
        delayedSaveJob?.cancel()
        localDraft.value = snapshot
        draftRepository.save(snapshot)
        return snapshot
    }

    companion object {
        private const val AUTO_SAVE_DEBOUNCE_MS = 300L

        fun factory(
            draftId: Long,
            draftRepository: DraftRepositoryContract,
            settingsRepository: SettingsRepositoryContract,
            assetStorage: AssetStorageContract,
            gitHubPublisher: GitHubPublisherContract,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(
                    draftId = draftId,
                    draftRepository = draftRepository,
                    settingsRepository = settingsRepository,
                    assetStorage = assetStorage,
                    gitHubPublisher = gitHubPublisher,
                ) as T
            }
        }
    }
}
