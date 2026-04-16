package com.blogmd.mizukiwriter.ui.feature.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import com.blogmd.mizukiwriter.domain.SiteConfigSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ConfigUiState(
    val snapshot: SiteConfigSnapshot = SiteConfigSnapshot(),
    val rawSource: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
)

class ConfigViewModel(
    private val settingsRepository: SettingsRepositoryContract,
    private val workspaceRepository: GitHubWorkspaceRepositoryContract,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            runCatching {
                val settings = settingsRepository.settings.first()
                val (snapshot, source) = workspaceRepository.loadSiteConfig(settings)
                _uiState.value = ConfigUiState(
                    snapshot = snapshot,
                    rawSource = source,
                    isLoading = false,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = error.message ?: "加载配置失败",
                )
            }
        }
    }

    fun updateRawSource(source: String) {
        _uiState.value = _uiState.value.copy(rawSource = source)
    }

    fun save(snapshot: SiteConfigSnapshot) {
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.settings.first()
                workspaceRepository.saveSiteConfig(settings, snapshot, _uiState.value.rawSource)
                _uiState.value = _uiState.value.copy(
                    snapshot = snapshot,
                    message = "配置已保存",
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(message = error.message ?: "保存配置失败")
            }
        }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepositoryContract,
            workspaceRepository: GitHubWorkspaceRepositoryContract,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ConfigViewModel(settingsRepository, workspaceRepository) as T
            }
        }
    }
}
