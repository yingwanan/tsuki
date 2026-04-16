package com.blogmd.mizukiwriter.ui.feature.settings

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
class SettingsViewModelTest {
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
    fun `save persists background image path and clearing it`() = runTest(dispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.save(repository.state.value.copy(backgroundImagePath = "/data/user/0/app/files/backgrounds/hero.webp"))
        runCurrent()
        assertThat(repository.savedSettings?.backgroundImagePath)
            .isEqualTo("/data/user/0/app/files/backgrounds/hero.webp")

        viewModel.save(repository.state.value.copy(backgroundImagePath = ""))
        runCurrent()
        assertThat(repository.savedSettings?.backgroundImagePath).isEmpty()
    }

    private class FakeSettingsRepository : SettingsRepositoryContract {
        val state = MutableStateFlow(
            GitHubSettings(
                owner = "demo",
                repo = "blog",
                branch = "master",
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
