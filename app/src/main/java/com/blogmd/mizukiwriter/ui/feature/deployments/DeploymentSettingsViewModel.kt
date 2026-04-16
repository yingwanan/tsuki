package com.blogmd.mizukiwriter.ui.feature.deployments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class DeploymentSettingsViewModel(
    private val settingsRepository: SettingsRepositoryContract,
) : ViewModel() {
    val uiState: StateFlow<GitHubSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, GitHubSettings())

    private val _savedMessage = MutableStateFlow<String?>(null)
    val savedMessage: StateFlow<String?> = _savedMessage.asStateFlow()

    fun save(settings: GitHubSettings) {
        viewModelScope.launch {
            settingsRepository.save(settings)
            _savedMessage.value = "部署设置已保存"
        }
    }

    fun consumeSavedMessage() {
        _savedMessage.value = null
    }

    companion object {
        fun factory(settingsRepository: SettingsRepositoryContract): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DeploymentSettingsViewModel(settingsRepository) as T
            }
        }
    }
}
