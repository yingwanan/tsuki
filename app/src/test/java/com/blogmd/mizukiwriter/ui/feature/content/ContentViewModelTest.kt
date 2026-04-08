package com.blogmd.mizukiwriter.ui.feature.content

import com.blogmd.mizukiwriter.data.github.GitHubRepositoryResponse
import com.blogmd.mizukiwriter.data.github.GitHubDeleteResult
import com.blogmd.mizukiwriter.data.github.GitHubPublishResult
import com.blogmd.mizukiwriter.data.github.GitHubPublisherContract
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
import com.blogmd.mizukiwriter.data.github.RemoteContentItem
import com.blogmd.mizukiwriter.data.github.RemoteContentType
import com.blogmd.mizukiwriter.data.github.RemoteFileDocument
import com.blogmd.mizukiwriter.data.media.AssetStorageContract
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.repository.DraftRepositoryContract
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import com.blogmd.mizukiwriter.domain.SiteConfigSnapshot
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContentViewModelTest {
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
    fun `refresh loads local drafts and remote items`() = runTest(dispatcher) {
        val viewModel = ContentViewModel(
            draftRepository = FakeDraftRepository(),
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = FakeGitHubPublisher(),
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.refreshRemoteContent()
        runCurrent()

        assertThat(viewModel.localDrafts.value).hasSize(1)
        assertThat(viewModel.remoteItems.value).hasSize(2)
        val remote = viewModel.remoteItems.value.first()
        assertThat(remote.path).isEqualTo("src/content/posts/hello-world.md")
        assertThat(remote.title).isEqualTo("远程标题")
        assertThat(remote.description).isEqualTo("远程简介")
        assertThat(viewModel.remoteItems.value.last().draft).isTrue()
    }

    @Test
    fun `deleteLocalDraft removes local draft assets`() = runTest(dispatcher) {
        val draftRepository = FakeDraftRepository()
        val assetStorage = FakeAssetStorage()
        val viewModel = ContentViewModel(
            draftRepository = draftRepository,
            settingsRepository = FakeSettingsRepository(),
            assetStorage = assetStorage,
            gitHubPublisher = FakeGitHubPublisher(),
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.deleteLocalDraft(1L)
        runCurrent()

        assertThat(draftRepository.deletedIds).containsExactly(1L)
        assertThat(assetStorage.deletedAssetIds).containsExactly(1L)
    }

    @Test
    fun `deleteRemoteArticle delegates exact remote path`() = runTest(dispatcher) {
        val publisher = FakeGitHubPublisher()
        val viewModel = ContentViewModel(
            draftRepository = FakeDraftRepository(),
            settingsRepository = FakeSettingsRepository(),
            assetStorage = FakeAssetStorage(),
            gitHubPublisher = publisher,
            workspaceRepository = FakeWorkspaceRepository(),
        )

        runCurrent()
        viewModel.deleteRemoteArticle("src/content/posts/hello-world/index.md")
        runCurrent()

        assertThat(publisher.deletedRemotePaths).containsExactly("src/content/posts/hello-world/index.md")
    }

    private class FakeDraftRepository : DraftRepositoryContract {
        val deletedIds = mutableListOf<Long>()

        override fun observeAll(): Flow<List<DraftPost>> = flowOf(listOf(DraftPost(id = 1L, title = "Draft")))
        override fun observeById(id: Long): Flow<DraftPost?> = flowOf(null)
        override suspend fun createBlankDraft(): Long = 0L
        override suspend fun getById(id: Long): DraftPost? = null
        override suspend fun save(draft: DraftPost): Long = draft.id
        override suspend fun markPublishSuccess(draft: DraftPost, slug: String) = Unit
        override suspend fun markPublishFailure(draft: DraftPost, message: String) = Unit
        override suspend fun delete(draftId: Long) {
            deletedIds += draftId
        }
    }

    private class FakeSettingsRepository : SettingsRepositoryContract {
        override val settings: Flow<GitHubSettings> = flowOf(
            GitHubSettings(owner = "demo", repo = "blog", personalAccessToken = "token"),
        )

        override suspend fun save(settings: GitHubSettings) = Unit
    }

    private class FakeAssetStorage : AssetStorageContract {
        val deletedAssetIds = mutableListOf<Long>()

        override fun importAsset(
            draftId: Long,
            sourceUri: android.net.Uri,
            resolver: android.content.ContentResolver,
            preferredBaseName: String?,
        ): String = error("Not used")

        override fun listAssetNames(draftId: Long): List<String> = emptyList()

        override fun listAssetFiles(draftId: Long): List<java.io.File> = emptyList()

        override fun deleteDraftAssets(draftId: Long) {
            deletedAssetIds += draftId
        }
    }

    private class FakeGitHubPublisher : GitHubPublisherContract {
        val deletedRemotePaths = mutableListOf<String>()

        override suspend fun publish(
            draft: DraftPost,
            settings: GitHubSettings,
            overwrite: Boolean,
        ): GitHubPublishResult = error("Not used")

        override suspend fun publishRemoteArticle(
            draft: DraftPost,
            settings: GitHubSettings,
            remotePath: String,
        ): GitHubPublishResult = error("Not used")

        override suspend fun deleteRemoteArticle(
            draft: DraftPost,
            settings: GitHubSettings,
        ): GitHubDeleteResult = error("Not used")

        override suspend fun deleteRemoteArticleByPath(
            articlePath: String,
            settings: GitHubSettings,
        ): GitHubDeleteResult {
            deletedRemotePaths += articlePath
            return GitHubDeleteResult.Success(articlePath)
        }
    }

    private class FakeWorkspaceRepository : GitHubWorkspaceRepositoryContract {
        override suspend fun listRemoteContent(settings: GitHubSettings) = listOf(
            RemoteContentItem("src/content/spec/about.md", "sha", RemoteContentType.Page),
            RemoteContentItem(
                path = "src/content/posts/hello-world.md",
                sha = "sha-post",
                type = RemoteContentType.Post,
                title = "远程标题",
                description = "远程简介",
            ),
            RemoteContentItem(
                path = "src/content/posts/draft-note.md",
                sha = "sha-draft",
                type = RemoteContentType.Post,
                title = "远程草稿",
                description = "未发布",
                draft = true,
            ),
        )

        override suspend fun loadFile(settings: GitHubSettings, path: String) = RemoteFileDocument(path, "sha", "", "main")
        override suspend fun saveFile(settings: GitHubSettings, path: String, content: String, commitMessage: String) =
            error("Not used")

        override suspend fun deleteFile(settings: GitHubSettings, path: String, commitMessage: String) =
            error("Not used")

        override suspend fun loadSiteConfig(settings: GitHubSettings) = SiteConfigSnapshot() to ""
        override suspend fun saveSiteConfig(settings: GitHubSettings, snapshot: SiteConfigSnapshot, source: String) =
            error("Not used")

        override suspend fun listDeployments(settings: GitHubSettings) = emptyList<com.blogmd.mizukiwriter.data.github.DeploymentRecord>()
        override suspend fun loadRepository(settings: GitHubSettings) = GitHubRepositoryResponse(fullName = "demo/blog")
    }
}
