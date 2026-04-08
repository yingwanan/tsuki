package com.blogmd.mizukiwriter.ui.feature.editor

import android.content.ContentResolver
import android.net.Uri
import com.blogmd.mizukiwriter.data.github.GitHubDeleteResult
import com.blogmd.mizukiwriter.data.github.GitHubPublishResult
import com.blogmd.mizukiwriter.data.github.GitHubPublisherContract
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
import com.blogmd.mizukiwriter.data.github.RemoteContentItem
import com.blogmd.mizukiwriter.data.github.RemoteFileDocument
import com.blogmd.mizukiwriter.data.github.DeploymentRecord
import com.blogmd.mizukiwriter.data.github.GitHubContentResponse
import com.blogmd.mizukiwriter.data.github.GitHubRepositoryResponse
import com.blogmd.mizukiwriter.data.media.AssetStorageContract
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.model.PublishState
import com.blogmd.mizukiwriter.data.repository.DraftRepositoryContract
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import com.blogmd.mizukiwriter.domain.SiteConfigSnapshot
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
class EditorViewModelTest {
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
    fun `publish fails fast when required metadata is missing`() = runTest(dispatcher) {
        val repository = FakeDraftRepository(
            DraftPost(
                id = 7L,
                title = "",
                description = "",
                published = "2025-01-20",
            ),
        )
        val publisher = FakeGitHubPublisher()
        val viewModel = EditorViewModel(
            draftId = 7L,
            draftRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = publisher,
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.publish()
        runCurrent()

        assertThat(viewModel.message.value).contains("标题")
        assertThat(publisher.publishCalls).isEqualTo(0)
    }

    @Test
    fun `deleteDraft removes local draft without touching remote when remote delete not requested`() = runTest(dispatcher) {
        val repository = FakeDraftRepository(
            DraftPost(
                id = 9L,
                title = "Title",
                description = "Description",
                published = "2025-01-20",
                slug = "published-post",
                publishState = PublishState.Synced,
            ),
        )
        val publisher = FakeGitHubPublisher()
        val viewModel = EditorViewModel(
            draftId = 9L,
            draftRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = publisher,
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.deleteDraft(deleteRemote = false)
        runCurrent()

        assertThat(repository.deletedIds).containsExactly(9L)
        assertThat(publisher.deleteCalls).isEqualTo(0)
    }

    @Test
    fun `deleteDraft still removes remote article when delete requested and slug exists after failed sync`() = runTest(dispatcher) {
        val repository = FakeDraftRepository(
            DraftPost(
                id = 10L,
                title = "Title",
                description = "Description",
                published = "2025-01-20",
                slug = "published-post",
                publishState = PublishState.Failed,
                lastPublishError = "GitHub 请求失败: 403",
            ),
        )
        val publisher = FakeGitHubPublisher()
        val viewModel = EditorViewModel(
            draftId = 10L,
            draftRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = publisher,
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.deleteDraft(deleteRemote = true)
        runCurrent()

        assertThat(repository.deletedIds).containsExactly(10L)
        assertThat(publisher.deleteCalls).isEqualTo(1)
    }

    @Test
    fun `publish automatically overwrites when draft has already been synced`() = runTest(dispatcher) {
        val repository = FakeDraftRepository(
            DraftPost(
                id = 11L,
                title = "Title",
                description = "Description",
                published = "2025-01-20",
                slug = "published-post",
                publishState = PublishState.Synced,
            ),
        )
        val publisher = FakeGitHubPublisher()
        val viewModel = EditorViewModel(
            draftId = 11L,
            draftRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = publisher,
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.publish()
        runCurrent()

        assertThat(publisher.publishCalls).isEqualTo(1)
        assertThat(publisher.lastOverwrite).isTrue()
    }

    @Test
    fun `publish automatically overwrites when draft has slug even after previous failure`() = runTest(dispatcher) {
        val repository = FakeDraftRepository(
            DraftPost(
                id = 12L,
                title = "Title",
                description = "Description",
                published = "2025-01-20",
                slug = "published-post",
                publishState = PublishState.Failed,
                lastPublishError = "GitHub 请求失败: 422",
            ),
        )
        val publisher = FakeGitHubPublisher()
        val viewModel = EditorViewModel(
            draftId = 12L,
            draftRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = publisher,
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.publish()
        runCurrent()

        assertThat(publisher.publishCalls).isEqualTo(1)
        assertThat(publisher.lastOverwrite).isTrue()
    }

    @Test
    fun `publish deletes local draft after successful local publish`() = runTest(dispatcher) {
        val repository = FakeDraftRepository(
            DraftPost(
                id = 13L,
                title = "Title",
                description = "Description",
                published = "2025-01-20",
            ),
        )
        val publisher = FakeGitHubPublisher()
        val assetStorage = FakeAssetStorage()
        val viewModel = EditorViewModel(
            draftId = 13L,
            remoteArticlePath = null,
            remoteArticleTitle = null,
            draftRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = assetStorage,
            gitHubPublisher = publisher,
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.publish()
        runCurrent()

        assertThat(repository.deletedIds).containsExactly(13L)
        assertThat(assetStorage.deletedAssetIds).containsExactly(13L)
        assertThat(viewModel.draftDeleted.value).isTrue()
    }

    @Test
    fun `remote editor loads remote markdown and publishes back to exact path`() = runTest(dispatcher) {
        val repository = FakeDraftRepository()
        val publisher = FakeGitHubPublisher()
        val workspaceRepository = FakeWorkspaceRepository(
            remoteDocument = RemoteFileDocument(
                path = "src/content/posts/hello-world/index.md",
                sha = "sha-1",
                branch = "main",
                content = """
                    ---
                    title: 远程标题
                    published: 2025-01-20
                    description: 远程简介
                    ---

                    正文
                """.trimIndent(),
            ),
        )
        val viewModel = EditorViewModel(
            draftId = 0L,
            remoteArticlePath = "src/content/posts/hello-world/index.md",
            remoteArticleTitle = "远程标题",
            draftRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = publisher,
            workspaceRepository = workspaceRepository,
        )

        runCurrent()
        assertThat(viewModel.draft.value?.title).isEqualTo("远程标题")

        viewModel.publish()
        runCurrent()

        assertThat(publisher.publishRemoteCalls).isEqualTo(1)
        assertThat(publisher.lastRemotePath).isEqualTo("src/content/posts/hello-world/index.md")
        assertThat(repository.deletedIds).isEmpty()
        assertThat(viewModel.draftDeleted.value).isFalse()
    }

    private class FakeDraftRepository(initial: DraftPost? = null) : DraftRepositoryContract {
        private val drafts = MutableStateFlow(initial?.let { mapOf(it.id to it) } ?: emptyMap())
        val deletedIds = mutableListOf<Long>()

        override fun observeAll(): Flow<List<DraftPost>> = drafts.map { it.values.toList() }

        override fun observeById(id: Long): Flow<DraftPost?> = drafts.map { it[id] }

        override suspend fun createBlankDraft(): Long = 999L

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
        var publishCalls = 0
        var publishRemoteCalls = 0
        var deleteCalls = 0
        var lastOverwrite = false
        var lastRemotePath: String? = null

        override suspend fun publish(
            draft: DraftPost,
            settings: GitHubSettings,
            overwrite: Boolean,
        ): GitHubPublishResult {
            publishCalls++
            lastOverwrite = overwrite
            return GitHubPublishResult.Success(draft.slug.ifBlank { "slug" })
        }

        override suspend fun publishRemoteArticle(
            draft: DraftPost,
            settings: GitHubSettings,
            remotePath: String,
        ): GitHubPublishResult {
            publishRemoteCalls++
            lastRemotePath = remotePath
            return GitHubPublishResult.Success(draft.slug.ifBlank { "slug" })
        }

        override suspend fun deleteRemoteArticle(draft: DraftPost, settings: GitHubSettings): GitHubDeleteResult {
            deleteCalls++
            return GitHubDeleteResult.Success(draft.slug)
        }

        override suspend fun deleteRemoteArticleByPath(articlePath: String, settings: GitHubSettings): GitHubDeleteResult {
            deleteCalls++
            lastRemotePath = articlePath
            return GitHubDeleteResult.Success(articlePath.substringBeforeLast('/').substringAfterLast('/'))
        }
    }

    private class FakeAssetStorage : AssetStorageContract {
        val deletedAssetIds = mutableListOf<Long>()
        override fun importAsset(
            draftId: Long,
            sourceUri: Uri,
            resolver: ContentResolver,
            preferredBaseName: String?,
        ): String = error("Not used")

        override fun listAssetNames(draftId: Long): List<String> = emptyList()

        override fun listAssetFiles(draftId: Long): List<File> = emptyList()

        override fun deleteDraftAssets(draftId: Long) {
            deletedAssetIds += draftId
        }
    }

    private class FakeWorkspaceRepository(
        private val remoteDocument: RemoteFileDocument = RemoteFileDocument(
            path = "",
            sha = "",
            branch = "",
            content = "",
        ),
    ) : GitHubWorkspaceRepositoryContract {
        override suspend fun listRemoteContent(settings: GitHubSettings): List<RemoteContentItem> = emptyList()
        override suspend fun loadFile(settings: GitHubSettings, path: String): RemoteFileDocument = remoteDocument
        override suspend fun saveFile(
            settings: GitHubSettings,
            path: String,
            content: String,
            commitMessage: String,
        ): GitHubContentResponse = GitHubContentResponse()

        override suspend fun deleteFile(settings: GitHubSettings, path: String, commitMessage: String): GitHubContentResponse =
            GitHubContentResponse()

        override suspend fun loadSiteConfig(settings: GitHubSettings): Pair<SiteConfigSnapshot, String> =
            SiteConfigSnapshot() to ""

        override suspend fun saveSiteConfig(
            settings: GitHubSettings,
            snapshot: SiteConfigSnapshot,
            source: String,
        ): GitHubContentResponse = GitHubContentResponse()

        override suspend fun listDeployments(settings: GitHubSettings): List<DeploymentRecord> = emptyList()
        override suspend fun loadRepository(settings: GitHubSettings): GitHubRepositoryResponse = GitHubRepositoryResponse()
    }
}
