package com.blogmd.mizukiwriter.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blogmd.mizukiwriter.data.deployment.DeploymentCenterRepositoryContract
import com.blogmd.mizukiwriter.data.repository.DraftRepositoryContract
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DashboardUiState(
    val providerLabel: String = "",
    val repositoryName: String = "",
    val defaultBranch: String = "",
    val productionDomain: String = "",
    val localDraftCount: Int = 0,
    val latestDeploymentName: String = "",
    val latestDeploymentStatus: String = "",
    val latestDeploymentUrl: String = "",
    val isRefreshing: Boolean = false,
    val message: String? = null,
)

class DashboardViewModel(
    private val settingsRepository: SettingsRepositoryContract,
    private val draftRepository: DraftRepositoryContract,
    private val deploymentRepository: DeploymentCenterRepositoryContract,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, message = null)
            runCatching {
                val settings = settingsRepository.settings.first()
                ensureConfigured(settings)
                val snapshot = deploymentRepository.loadSnapshot(settings)
                val drafts = draftRepository.observeAll().first()
                val latestDeployment = snapshot.recentDeployments.firstOrNull()
                _uiState.value = DashboardUiState(
                    providerLabel = snapshot.provider.label,
                    repositoryName = snapshot.repositoryName,
                    defaultBranch = settings.branch,
                    productionDomain = snapshot.productionDomain,
                    localDraftCount = drafts.size,
                    latestDeploymentName = latestDeployment?.name.orEmpty(),
                    latestDeploymentStatus = latestDeployment?.conclusion ?: latestDeployment?.status.orEmpty(),
                    latestDeploymentUrl = latestDeployment?.htmlUrl.orEmpty(),
                    isRefreshing = false,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    message = error.message ?: "加载控制台失败",
                )
            }
        }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepositoryContract,
            draftRepository: DraftRepositoryContract,
            deploymentRepository: DeploymentCenterRepositoryContract,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(settingsRepository, draftRepository, deploymentRepository) as T
            }
        }
    }
}

private fun ensureConfigured(settings: GitHubSettings) {
    check(settings.owner.isNotBlank() && settings.repo.isNotBlank() && settings.personalAccessToken.isNotBlank()) {
        "请先完成 GitHub 仓库设置"
    }
}
