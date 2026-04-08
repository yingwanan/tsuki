package com.blogmd.mizukiwriter.ui.feature.deployments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blogmd.mizukiwriter.data.deployment.DeploymentCenterRepositoryContract
import com.blogmd.mizukiwriter.data.github.DeploymentRecord
import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform
import com.blogmd.mizukiwriter.data.settings.SettingsRepositoryContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DeploymentsUiState(
    val selectedPlatform: DeploymentPlatform = DeploymentPlatform.Vercel,
    val availablePlatforms: List<DeploymentPlatform> = DeploymentPlatform.entries,
    val providerLabel: String = "",
    val repositoryName: String = "",
    val repositoryBranch: String = "",
    val projectName: String = "",
    val projectId: String = "",
    val productionDomain: String = "",
    val previewDomain: String = "",
    val customDomain: String = "",
    val customDomainStatus: String = "",
    val consoleUrl: String = "",
    val setupHint: String = "",
    val deployments: List<DeploymentRecord> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
)

class DeploymentsViewModel(
    private val settingsRepository: SettingsRepositoryContract,
    private val deploymentRepository: DeploymentCenterRepositoryContract,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeploymentsUiState())
    val uiState: StateFlow<DeploymentsUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            runCatching {
                val settings = settingsRepository.settings.first()
                val snapshot = deploymentRepository.loadSnapshot(settings)
                _uiState.value = DeploymentsUiState(
                    selectedPlatform = settings.deploymentPlatform,
                    providerLabel = snapshot.provider.label,
                    repositoryName = snapshot.repositoryName,
                    repositoryBranch = settings.branch,
                    projectName = snapshot.projectName,
                    projectId = snapshot.projectId,
                    productionDomain = snapshot.productionDomain,
                    previewDomain = snapshot.previewDomain,
                    customDomain = snapshot.customDomain,
                    customDomainStatus = snapshot.customDomainStatus,
                    consoleUrl = snapshot.consoleUrl,
                    setupHint = snapshot.setupHint,
                    deployments = snapshot.recentDeployments,
                    isLoading = false,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = error.message ?: "加载部署状态失败",
                )
            }
        }
    }

    fun selectPlatform(platform: DeploymentPlatform) {
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.settings.first()
                settingsRepository.save(settings.copy(deploymentPlatform = platform))
                val snapshot = deploymentRepository.loadSnapshot(settings.copy(deploymentPlatform = platform))
                _uiState.value = DeploymentsUiState(
                    selectedPlatform = platform,
                    providerLabel = snapshot.provider.label,
                    repositoryName = snapshot.repositoryName,
                    repositoryBranch = settings.branch,
                    projectName = snapshot.projectName,
                    projectId = snapshot.projectId,
                    productionDomain = snapshot.productionDomain,
                    previewDomain = snapshot.previewDomain,
                    customDomain = snapshot.customDomain,
                    customDomainStatus = snapshot.customDomainStatus,
                    consoleUrl = snapshot.consoleUrl,
                    setupHint = snapshot.setupHint,
                    deployments = snapshot.recentDeployments,
                    isLoading = false,
                    message = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(message = error.message ?: "切换部署平台失败")
            }
        }
    }

    fun createProject() {
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.settings.first()
                deploymentRepository.createProject(settings)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(message = result.message)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(message = error.message ?: "创建部署项目失败")
            }
        }
    }

    fun bindCustomDomain() {
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.settings.first()
                deploymentRepository.bindDomain(settings, settings.deploymentCustomDomain)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(message = result.message)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(message = error.message ?: "绑定自定义域名失败")
            }
        }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepositoryContract,
            deploymentRepository: DeploymentCenterRepositoryContract,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DeploymentsViewModel(settingsRepository, deploymentRepository) as T
            }
        }
    }
}
