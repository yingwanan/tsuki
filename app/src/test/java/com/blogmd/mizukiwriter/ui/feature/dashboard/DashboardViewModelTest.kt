package com.blogmd.mizukiwriter.ui.feature.dashboard

import com.blogmd.mizukiwriter.data.deployment.DeploymentActionResult
import com.blogmd.mizukiwriter.data.deployment.DeploymentCenterRepositoryContract
import com.blogmd.mizukiwriter.data.deployment.DeploymentCenterSnapshot
import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform
import com.blogmd.mizukiwriter.data.github.DeploymentRecord
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.repository.DraftRepositoryContract
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class DashboardViewModelTest {
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
    fun `refresh loads repository deployment and local draft summary`() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(
            settingsRepository = FakeSettingsRepository(),
            draftRepository = FakeDraftRepository(),
            deploymentRepository = FakeDeploymentRepository(),
        )

        runCurrent()
        viewModel.refresh()
        runCurrent()

        assertThat(viewModel.uiState.value.repositoryName).isEqualTo("demo/blog")
        assertThat(viewModel.uiState.value.localDraftCount).isEqualTo(2)
        assertThat(viewModel.uiState.value.providerLabel).isEqualTo("Vercel")
        assertThat(viewModel.uiState.value.productionDomain).isEqualTo("blog-console.vercel.app")
        assertThat(viewModel.uiState.value.latestDeploymentName).isEqualTo("Deploy to Pages")
    }

    private class FakeSettingsRepository : SettingsRepositoryContract {
        override val settings: Flow<GitHubSettings> = flowOf(
            GitHubSettings(
                owner = "demo",
                repo = "blog",
                personalAccessToken = "token",
                deploymentPlatform = DeploymentPlatform.Vercel,
                deploymentProjectName = "blog-console",
            ),
        )

        override suspend fun save(settings: GitHubSettings) = Unit
    }

    private class FakeDraftRepository : DraftRepositoryContract {
        override fun observeAll(): Flow<List<DraftPost>> = flowOf(
            listOf(DraftPost(id = 1L), DraftPost(id = 2L)),
        )

        override fun observeById(id: Long): Flow<DraftPost?> = flowOf(null)
        override suspend fun createBlankDraft(): Long = 0L
        override suspend fun getById(id: Long): DraftPost? = null
        override suspend fun save(draft: DraftPost): Long = draft.id
        override suspend fun markPublishSuccess(draft: DraftPost, slug: String) = Unit
        override suspend fun markPublishFailure(draft: DraftPost, message: String) = Unit
        override suspend fun delete(draftId: Long) = Unit
    }

    private class FakeDeploymentRepository : DeploymentCenterRepositoryContract {
        override suspend fun loadSnapshot(settings: GitHubSettings) = DeploymentCenterSnapshot(
            provider = DeploymentPlatform.Vercel,
            repositoryName = "demo/blog",
            projectName = "blog-console",
            productionDomain = "blog-console.vercel.app",
            previewDomain = "preview-blog-console.vercel.app",
            recentDeployments = listOf(
                DeploymentRecord(
                    id = 1L,
                    name = "Deploy to Pages",
                    status = "completed",
                    conclusion = "success",
                    htmlUrl = "https://github.com/demo/blog/actions/runs/1",
                    branch = "main",
                    updatedAt = "2026-04-05T10:00:00Z",
                ),
            ),
        )

        override suspend fun createProject(settings: GitHubSettings) =
            DeploymentActionResult(success = true, message = "unused")

        override suspend fun bindDomain(settings: GitHubSettings, domain: String) =
            DeploymentActionResult(success = true, message = "unused")
    }
}
