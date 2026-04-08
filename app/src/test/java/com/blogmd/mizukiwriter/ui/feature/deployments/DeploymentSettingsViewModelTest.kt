package com.blogmd.mizukiwriter.ui.feature.deployments

import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform
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
class DeploymentSettingsViewModelTest {
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
    fun `save updates deployment fields while preserving github settings`() = runTest(dispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = DeploymentSettingsViewModel(repository)

        viewModel.save(
            repository.state.value.copy(
                deploymentPlatform = DeploymentPlatform.CloudflarePages,
                deploymentProjectName = "blog-console-pages",
                deploymentAccountId = "account-123",
            ),
        )
        runCurrent()

        val saved = repository.savedSettings
        assertThat(saved?.owner).isEqualTo("demo")
        assertThat(saved?.repo).isEqualTo("blog")
        assertThat(saved?.deploymentPlatform).isEqualTo(DeploymentPlatform.CloudflarePages)
        assertThat(saved?.deploymentProjectName).isEqualTo("blog-console-pages")
        assertThat(saved?.deploymentAccountId).isEqualTo("account-123")
    }

    private class FakeSettingsRepository : SettingsRepositoryContract {
        val state = MutableStateFlow(
            GitHubSettings(
                owner = "demo",
                repo = "blog",
                branch = "master",
                personalAccessToken = "github-token",
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
}
