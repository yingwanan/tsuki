package com.blogmd.mizukiwriter.ui.feature.repositoryfile

import com.blogmd.mizukiwriter.data.github.GitHubContentResponse
import com.blogmd.mizukiwriter.data.github.GitHubRepositoryResponse
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
import com.blogmd.mizukiwriter.data.github.RemoteFileDocument
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
class RepositoryFileViewModelTest {
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
    fun `refresh loads remote file document`() = runTest(dispatcher) {
        val repository = FakeWorkspaceRepository()
        val viewModel = RepositoryFileViewModel(
            path = "src/content/spec/about.md",
            title = "关于页",
            bindingName = null,
            settingsRepository = FakeSettingsRepository(),
            workspaceRepository = repository,
        )

        runCurrent()
        viewModel.refresh()
        runCurrent()

        assertThat(viewModel.uiState.value.path).isEqualTo("src/content/spec/about.md")
        assertThat(viewModel.uiState.value.markdownBody).contains("About page")
    }

    @Test
    fun `save writes updated content back to repository`() = runTest(dispatcher) {
        val repository = FakeWorkspaceRepository()
        val viewModel = RepositoryFileViewModel(
            path = "src/content/spec/about.md",
            title = "关于页",
            bindingName = null,
            settingsRepository = FakeSettingsRepository(),
            workspaceRepository = repository,
        )

        runCurrent()
        viewModel.refresh()
        runCurrent()
        viewModel.saveMarkdown(
            frontmatter = viewModel.uiState.value.markdownFrontmatter,
            body = "# Updated",
        )
        runCurrent()

        assertThat(repository.savedContents.single().trim()).isEqualTo("# Updated")
    }

    private class FakeSettingsRepository : SettingsRepositoryContract {
        override val settings: Flow<GitHubSettings> = flowOf(
            GitHubSettings(owner = "demo", repo = "blog", personalAccessToken = "token"),
        )

        override suspend fun save(settings: GitHubSettings) = Unit
    }

    private class FakeWorkspaceRepository : GitHubWorkspaceRepositoryContract {
        val savedContents = mutableListOf<String>()

        override suspend fun listRemoteContent(settings: GitHubSettings) = emptyList<com.blogmd.mizukiwriter.data.github.RemoteContentItem>()

        override suspend fun loadFile(settings: GitHubSettings, path: String) = RemoteFileDocument(
            path = path,
            sha = "file-sha",
            content = "# About page",
            branch = "main",
        )

        override suspend fun saveFile(
            settings: GitHubSettings,
            path: String,
            content: String,
            commitMessage: String,
        ): GitHubContentResponse {
            savedContents += content
            return GitHubContentResponse(sha = "saved")
        }

        override suspend fun deleteFile(settings: GitHubSettings, path: String, commitMessage: String) =
            GitHubContentResponse(sha = "deleted")

        override suspend fun loadSiteConfig(settings: GitHubSettings) = SiteConfigSnapshot() to ""
        override suspend fun saveSiteConfig(settings: GitHubSettings, snapshot: SiteConfigSnapshot, source: String) = GitHubContentResponse(sha = "saved")
        override suspend fun listDeployments(settings: GitHubSettings) = emptyList<com.blogmd.mizukiwriter.data.github.DeploymentRecord>()
        override suspend fun loadRepository(settings: GitHubSettings) = GitHubRepositoryResponse(fullName = "demo/blog")
    }
}
