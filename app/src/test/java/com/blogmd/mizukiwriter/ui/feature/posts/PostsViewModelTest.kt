package com.blogmd.mizukiwriter.ui.feature.posts

import android.content.ContentResolver
import android.net.Uri
import com.blogmd.mizukiwriter.data.github.GitHubDeleteResult
import com.blogmd.mizukiwriter.data.github.GitHubPublishResult
import com.blogmd.mizukiwriter.data.github.GitHubPublisherContract
import com.blogmd.mizukiwriter.data.media.AssetStorageContract
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.model.PublishState
import com.blogmd.mizukiwriter.data.repository.DraftRepositoryContract
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PostsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteDraft still removes remote article when delete requested and slug exists after failed sync`() = runTest(dispatcher) {
        val repository = FakeDraftRepository(
            DraftPost(
                id = 5L,
                title = "Title",
                description = "Description",
                published = "2025-01-20",
                slug = "published-post",
                publishState = PublishState.Failed,
                lastPublishError = "GitHub 请求失败: 403",
            ),
        )
        val publisher = FakeGitHubPublisher()
        val viewModel = PostsViewModel(
            draftRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = publisher,
        )

        runCurrent()
        viewModel.deleteDraft(repository.requireDraft(5L), deleteRemote = true)
        runCurrent()

        assertThat(repository.deletedIds).containsExactly(5L)
        assertThat(publisher.deleteCalls).isEqualTo(1)
    }

    private class FakeDraftRepository(initial: DraftPost) : DraftRepositoryContract {
        private val drafts = MutableStateFlow(mapOf(initial.id to initial))
        val deletedIds = mutableListOf<Long>()

        fun requireDraft(id: Long): DraftPost = drafts.value.getValue(id)

        override fun observeAll(): Flow<List<DraftPost>> = drafts.map { it.values.toList() }

        override fun observeById(id: Long): Flow<DraftPost?> = drafts.map { it[id] }

        override suspend fun createBlankDraft(): Long = error("Not used")

        override suspend fun getById(id: Long): DraftPost? = drafts.value[id]

        override suspend fun save(draft: DraftPost): Long {
            drafts.value = drafts.value + (draft.id to draft)
            return draft.id
        }

        override suspend fun markPublishSuccess(draft: DraftPost, slug: String) {
            save(draft.copy(slug = slug, publishState = PublishState.Synced))
        }

        override suspend fun markPublishFailure(draft: DraftPost, message: String) {
            save(draft.copy(publishState = PublishState.Failed, lastPublishError = message))
        }

        override suspend fun delete(draftId: Long) {
            deletedIds += draftId
            drafts.value = drafts.value - draftId
        }
    }

    private class FakeSettingsRepository : SettingsRepositoryContract {
        override val settings: Flow<GitHubSettings> = MutableStateFlow(
            GitHubSettings(owner = "demo", repo = "blog", personalAccessToken = "token"),
        )

        override suspend fun save(settings: GitHubSettings) = Unit
    }

    private class FakeGitHubPublisher : GitHubPublisherContract {
        var deleteCalls = 0

        override suspend fun publish(
            draft: DraftPost,
            settings: GitHubSettings,
            overwrite: Boolean,
        ): GitHubPublishResult = error("Not used")

        override suspend fun deleteRemoteArticle(draft: DraftPost, settings: GitHubSettings): GitHubDeleteResult {
            deleteCalls++
            return GitHubDeleteResult.Success(draft.slug)
        }
    }

    private class FakeAssetStorage : AssetStorageContract {
        override fun importAsset(
            draftId: Long,
            sourceUri: Uri,
            resolver: ContentResolver,
            preferredBaseName: String?,
        ): String = error("Not used")

        override fun listAssetNames(draftId: Long): List<String> = emptyList()

        override fun listAssetFiles(draftId: Long): List<File> = emptyList()

        override fun deleteDraftAssets(draftId: Long) = Unit
    }
}
