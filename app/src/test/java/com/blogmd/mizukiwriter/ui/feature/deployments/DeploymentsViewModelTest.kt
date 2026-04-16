package com.blogmd.mizukiwriter.ui.feature.deployments

import com.blogmd.mizukiwriter.data.deployment.DeploymentActionResult
import com.blogmd.mizukiwriter.data.deployment.DeploymentCenterRepositoryContract
import com.blogmd.mizukiwriter.data.deployment.DeploymentCenterSnapshot
import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform
import com.blogmd.mizukiwriter.data.github.DeploymentRecord
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeploymentsViewModelTest {
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
    fun `refresh loads deployment list`() = runTest(dispatcher) {
        val viewModel = DeploymentsViewModel(
            settingsRepository = FakeSettingsRepository(),
            deploymentRepository = FakeDeploymentRepository(),
        )

        runCurrent()
        viewModel.refresh()
        runCurrent()

        assertThat(viewModel.uiState.value.providerLabel).isEqualTo("Vercel")
        assertThat(viewModel.uiState.value.projectName).isEqualTo("blog-console")
        assertThat(viewModel.uiState.value.productionDomain).isEqualTo("blog-console.vercel.app")
        assertThat(viewModel.uiState.value.deployments).hasSize(2)
    }

    @Test
    fun `createProject surfaces success message`() = runTest(dispatcher) {
        val repository = FakeDeploymentRepository()
        val viewModel = DeploymentsViewModel(
            settingsRepository = FakeSettingsRepository(),
            deploymentRepository = repository,
        )

        viewModel.createProject()
        runCurrent()

        assertThat(repository.createProjectCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.message).isEqualTo("项目已创建")
    }

    @Test
    fun `selectPlatform persists chosen provider and reloads snapshot`() = runTest(dispatcher) {
        val settingsRepository = FakeSettingsRepository()
        val repository = FakeDeploymentRepository()
        val viewModel = DeploymentsViewModel(
            settingsRepository = settingsRepository,
            deploymentRepository = repository,
        )

        viewModel.selectPlatform(DeploymentPlatform.CloudflarePages)
        runCurrent()

        assertThat(settingsRepository.savedSettings?.deploymentPlatform).isEqualTo(DeploymentPlatform.CloudflarePages)
        assertThat(repository.lastLoadedSettings?.deploymentPlatform).isEqualTo(DeploymentPlatform.CloudflarePages)
        assertThat(viewModel.uiState.value.providerLabel).isEqualTo("Cloudflare Pages")
    }

    private class FakeSettingsRepository : SettingsRepositoryContract {
        private val state = MutableStateFlow(
            GitHubSettings(
                owner = "demo",
                repo = "blog",
                personalAccessToken = "token",
                deploymentPlatform = DeploymentPlatform.Vercel,
                deploymentProjectName = "blog-console",
            ),
        )
        var savedSettings: GitHubSettings? = null

        override val settings: Flow<GitHubSettings> = state

        override suspend fun save(settings: GitHubSettings) {
            savedSettings = settings
            state.value = settings
        }
    }

    private class FakeDeploymentRepository : DeploymentCenterRepositoryContract {
        var createProjectCalls = 0
        var lastLoadedSettings: GitHubSettings? = null

        override suspend fun loadSnapshot(settings: GitHubSettings): DeploymentCenterSnapshot {
            lastLoadedSettings = settings
            return when (settings.deploymentPlatform) {
                DeploymentPlatform.Vercel -> DeploymentCenterSnapshot(
                    provider = DeploymentPlatform.Vercel,
                    repositoryName = "demo/blog",
                    projectName = "blog-console",
                    productionDomain = "blog-console.vercel.app",
                    previewDomain = "preview-blog-console.vercel.app",
                    recentDeployments = listOf(
                        DeploymentRecord(1L, "Build", "completed", "success", "https://example.com/1", "main", "2026-04-05"),
                        DeploymentRecord(2L, "Preview", "in_progress", null, "https://example.com/2", "feature/x", "2026-04-05"),
                    ),
                )

                DeploymentPlatform.CloudflarePages -> DeploymentCenterSnapshot(
                    provider = DeploymentPlatform.CloudflarePages,
                    repositoryName = "demo/blog",
                    projectName = "blog-console-cf",
                    productionDomain = "blog-console.pages.dev",
                )

                DeploymentPlatform.EdgeOnePages -> DeploymentCenterSnapshot(
                    provider = DeploymentPlatform.EdgeOnePages,
                    repositoryName = "demo/blog",
                    projectName = "blog-console-edgeone",
                    productionDomain = "blog-console.edgeone.app",
                )
            }
        }

        override suspend fun createProject(settings: GitHubSettings): DeploymentActionResult {
            createProjectCalls += 1
            return DeploymentActionResult(success = true, message = "项目已创建")
        }

        override suspend fun bindDomain(settings: GitHubSettings, domain: String): DeploymentActionResult =
            DeploymentActionResult(success = true, message = "域名已绑定")
    }
}
