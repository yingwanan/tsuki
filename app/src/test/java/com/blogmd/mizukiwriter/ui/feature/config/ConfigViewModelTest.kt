package com.blogmd.mizukiwriter.ui.feature.config

import com.blogmd.mizukiwriter.data.github.GitHubContentResponse
import com.blogmd.mizukiwriter.data.github.GitHubRepositoryResponse
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
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
class ConfigViewModelTest {
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
    fun `refresh loads structured site settings and raw source`() = runTest(dispatcher) {
        val repository = FakeWorkspaceRepository()
        val viewModel = ConfigViewModel(
            settingsRepository = FakeSettingsRepository(),
            workspaceRepository = repository,
        )

        runCurrent()
        viewModel.refresh()
        runCurrent()

        assertThat(viewModel.uiState.value.snapshot.title).isEqualTo("Tsuki")
        assertThat(viewModel.uiState.value.rawSource).contains("const SITE_LANG")
    }

    @Test
    fun `save persists updated site snapshot`() = runTest(dispatcher) {
        val repository = FakeWorkspaceRepository()
        val viewModel = ConfigViewModel(
            settingsRepository = FakeSettingsRepository(),
            workspaceRepository = repository,
        )

        runCurrent()
        viewModel.refresh()
        runCurrent()
        viewModel.save(
            SiteConfigSnapshot(
                title = "Console",
                subtitle = "Suite",
                siteUrl = "https://example.com/",
                lang = "zh_CN",
                timeZone = 8,
            ),
        )
        runCurrent()

        assertThat(repository.savedSnapshots.single().title).isEqualTo("Console")
    }

    private class FakeSettingsRepository : SettingsRepositoryContract {
        override val settings: Flow<GitHubSettings> = flowOf(
            GitHubSettings(owner = "demo", repo = "blog", personalAccessToken = "token"),
        )

        override suspend fun save(settings: GitHubSettings) = Unit
    }

    private class FakeWorkspaceRepository : GitHubWorkspaceRepositoryContract {
        val savedSnapshots = mutableListOf<SiteConfigSnapshot>()

        override suspend fun listRemoteContent(settings: GitHubSettings) = emptyList<com.blogmd.mizukiwriter.data.github.RemoteContentItem>()
        override suspend fun loadFile(settings: GitHubSettings, path: String) = error("Not used")
        override suspend fun saveFile(settings: GitHubSettings, path: String, content: String, commitMessage: String) = error("Not used")
        override suspend fun deleteFile(settings: GitHubSettings, path: String, commitMessage: String) = error("Not used")

        override suspend fun loadSiteConfig(settings: GitHubSettings): Pair<SiteConfigSnapshot, String> =
            SiteConfigSnapshot(
                title = "Tsuki",
                subtitle = "Blog Console",
                siteUrl = "https://example.com/",
                lang = "ja",
                timeZone = 9,
            ) to """const SITE_LANG = "ja";"""

        override suspend fun saveSiteConfig(
            settings: GitHubSettings,
            snapshot: SiteConfigSnapshot,
            source: String,
        ): GitHubContentResponse {
            savedSnapshots += snapshot
            return GitHubContentResponse(sha = "saved")
        }

        override suspend fun listDeployments(settings: GitHubSettings) = emptyList<com.blogmd.mizukiwriter.data.github.DeploymentRecord>()
        override suspend fun loadRepository(settings: GitHubSettings) = GitHubRepositoryResponse(fullName = "demo/blog")
    }
}
