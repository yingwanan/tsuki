package com.blogmd.mizukiwriter.data.deployment

import com.blogmd.mizukiwriter.data.github.DeploymentRecord
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform
import com.blogmd.mizukiwriter.data.settings.EdgeOneExecutionMode
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class DeploymentCenterSnapshot(
    val provider: DeploymentPlatform,
    val repositoryName: String,
    val projectName: String = "",
    val projectId: String = "",
    val productionDomain: String = "",
    val previewDomain: String = "",
    val customDomain: String = "",
    val customDomainStatus: String = "",
    val consoleUrl: String = "",
    val setupHint: String = "",
    val recentDeployments: List<DeploymentRecord> = emptyList(),
)

data class DeploymentActionResult(
    val success: Boolean,
    val message: String,
)

@Serializable
data class VercelCreateProjectRequest(
    val name: String,
    val framework: String = "astro",
    @SerialName("gitRepository")
    val gitRepository: VercelGitRepository,
    @SerialName("buildCommand")
    val buildCommand: String,
    @SerialName("outputDirectory")
    val outputDirectory: String,
)

@Serializable
data class VercelGitRepository(
    val type: String = "github",
    val repo: String,
    @SerialName("productionBranch")
    val productionBranch: String,
)

@Serializable
data class VercelProject(
    val id: String,
    val name: String,
    val framework: String = "astro",
    @SerialName("latestDeployments") val latestDeployments: List<VercelDeployment>? = null,
    @SerialName("latestDomains")
    val latestDomains: List<String> = emptyList(),
    val targets: VercelProjectTargets = VercelProjectTargets(),
)

@Serializable
data class VercelProjectTargets(
    val production: VercelProjectTarget = VercelProjectTarget(),
    val preview: VercelProjectTarget = VercelProjectTarget(),
)

@Serializable
data class VercelProjectTarget(
    val alias: List<String> = emptyList(),
)

@Serializable
data class VercelAddDomainRequest(
    val name: String,
)

@Serializable
data class VercelDomainResponse(
    val name: String,
    @SerialName("apexName")
    val apexName: String,
)

@Serializable
data class VercelDeployment(
    val uid: String = "",
    val url: String = "",
)

interface VercelGateway {
    suspend fun getProject(projectName: String, teamId: String? = null): VercelProject
    suspend fun createProject(request: VercelCreateProjectRequest, teamId: String? = null): VercelProject
    suspend fun addDomain(projectName: String, request: VercelAddDomainRequest, teamId: String? = null): VercelDomainResponse
}

@Serializable
data class CloudflareEnvelope<T>(
    val result: T,
)

@Serializable
data class CloudflareProject(
    val name: String,
    val subdomain: String = "",
    val domains: List<String> = emptyList(),
)

@Serializable
data class CloudflareBuildConfig(
    @SerialName("build_command")
    val buildCommand: String,
    @SerialName("destination_dir")
    val destinationDir: String,
)

@Serializable
data class CloudflareCreateProjectRequest(
    val name: String,
    @SerialName("production_branch")
    val productionBranch: String,
    @SerialName("build_config")
    val buildConfig: CloudflareBuildConfig,
    @SerialName("source")
    val repository: CloudflareGitRepository,
)

@Serializable
data class CloudflareGitRepository(
    val type: String = "github",
    val repo: String,
)

@Serializable
data class CloudflareDomainRequest(
    val name: String,
)

@Serializable
data class CloudflareDomainResult(
    val name: String,
    val status: String,
)

interface CloudflarePagesGateway {
    suspend fun getProject(accountId: String, projectName: String): CloudflareEnvelope<CloudflareProject>
    suspend fun createProject(accountId: String, request: CloudflareCreateProjectRequest): CloudflareEnvelope<CloudflareProject>
    suspend fun addDomain(
        accountId: String,
        projectName: String,
        request: CloudflareDomainRequest,
    ): CloudflareEnvelope<CloudflareDomainResult>
}

data class EdgeOneWorkflowPlan(
    val path: String,
    val content: String,
    val summary: String,
)

interface EdgeOneWorkflowManager {
    fun buildWorkflow(settings: GitHubSettings): EdgeOneWorkflowPlan
}

interface DeploymentCenterRepositoryContract {
    suspend fun loadSnapshot(settings: GitHubSettings): DeploymentCenterSnapshot
    suspend fun createProject(settings: GitHubSettings): DeploymentActionResult
    suspend fun bindDomain(settings: GitHubSettings, domain: String): DeploymentActionResult
}

class DefaultEdgeOneWorkflowManager : EdgeOneWorkflowManager {
    override fun buildWorkflow(settings: GitHubSettings): EdgeOneWorkflowPlan {
        val projectName = settings.deploymentProjectName.ifBlank { settings.repo }
        val workflow = """
            name: Deploy EdgeOne Pages

            on:
              push:
                branches:
                  - ${settings.branch}
              workflow_dispatch:

            jobs:
              deploy:
                runs-on: ubuntu-latest
                steps:
                  - uses: actions/checkout@v4
                  - uses: actions/setup-node@v4
                    with:
                      node-version: 20
                  - run: corepack enable
                  - run: pnpm install --frozen-lockfile
                  - run: ${settings.deploymentBuildCommand}
                  - run: npx edgeone pages deploy -n $projectName -t ${'$'}{{ secrets.EDGEONE_API_TOKEN }}
        """.trimIndent()
        return EdgeOneWorkflowPlan(
            path = ".github/workflows/deploy-edgeone-pages.yml",
            content = workflow,
            summary = "已写入 EdgeOne Pages GitHub Actions 工作流，请在仓库 Secrets 中添加 EDGEONE_API_TOKEN。",
        )
    }
}

class DeploymentCenterRepository(
    private val workspaceRepository: GitHubWorkspaceRepositoryContract,
    private val vercelGatewayFactory: (String) -> VercelGateway,
    private val cloudflareGatewayFactory: (String) -> CloudflarePagesGateway,
    private val edgeOneWorkflowManager: EdgeOneWorkflowManager = DefaultEdgeOneWorkflowManager(),
) : DeploymentCenterRepositoryContract {
    override suspend fun loadSnapshot(settings: GitHubSettings): DeploymentCenterSnapshot {
        val history = runCatching { workspaceRepository.listDeployments(settings) }.getOrDefault(emptyList())
        val repository = runCatching { workspaceRepository.loadRepository(settings).fullName }
            .getOrDefault("${settings.owner}/${settings.repo}".trim('/'))
        return when (settings.deploymentPlatform) {
            DeploymentPlatform.Vercel -> loadVercelSnapshot(settings, repository, history)
            DeploymentPlatform.CloudflarePages -> loadCloudflareSnapshot(settings, repository, history)
            DeploymentPlatform.EdgeOnePages -> loadEdgeOneSnapshot(settings, repository, history)
        }
    }

    override suspend fun createProject(settings: GitHubSettings): DeploymentActionResult {
        return when (settings.deploymentPlatform) {
            DeploymentPlatform.Vercel -> createVercelProject(settings)
            DeploymentPlatform.CloudflarePages -> createCloudflareProject(settings)
            DeploymentPlatform.EdgeOnePages -> createEdgeOneWorkflow(settings)
        }
    }

    override suspend fun bindDomain(settings: GitHubSettings, domain: String): DeploymentActionResult {
        if (domain.isBlank()) return DeploymentActionResult(false, "请先填写自定义域名")
        return when (settings.deploymentPlatform) {
            DeploymentPlatform.Vercel -> {
                require(settings.deploymentProjectName.isNotBlank()) { "请先配置 Vercel 项目名" }
                val gateway = vercelGatewayFactory(settings.deploymentAccessToken)
                gateway.addDomain(
                    projectName = settings.deploymentProjectName,
                    request = VercelAddDomainRequest(name = domain),
                    teamId = settings.deploymentTeamId.ifBlank { null },
                )
                DeploymentActionResult(true, "已向 Vercel 提交域名绑定：$domain")
            }

            DeploymentPlatform.CloudflarePages -> {
                require(settings.deploymentAccountId.isNotBlank()) { "请先配置 Cloudflare Account ID" }
                require(settings.deploymentProjectName.isNotBlank()) { "请先配置 Cloudflare Pages 项目名" }
                val gateway = cloudflareGatewayFactory(settings.deploymentAccessToken)
                gateway.addDomain(
                    accountId = settings.deploymentAccountId,
                    projectName = settings.deploymentProjectName,
                    request = CloudflareDomainRequest(name = domain),
                )
                DeploymentActionResult(true, "已向 Cloudflare 提交域名绑定：$domain")
            }

            DeploymentPlatform.EdgeOnePages -> DeploymentActionResult(
                success = false,
                message = "EdgeOne 域名绑定暂未自动化，请在控制台完成绑定后回填状态。",
            )
        }
    }

    private suspend fun loadVercelSnapshot(
        settings: GitHubSettings,
        repositoryName: String,
        history: List<DeploymentRecord>,
    ): DeploymentCenterSnapshot {
        if (settings.deploymentAccessToken.isBlank() || settings.deploymentProjectName.isBlank()) {
            return DeploymentCenterSnapshot(
                provider = DeploymentPlatform.Vercel,
                repositoryName = repositoryName,
                projectName = settings.deploymentProjectName,
                projectId = settings.deploymentProjectId,
                productionDomain = settings.deploymentProductionDomain,
                previewDomain = settings.deploymentPreviewDomain,
                customDomain = settings.deploymentCustomDomain,
                customDomainStatus = settings.deploymentCustomDomainStatus,
                setupHint = "填写 Vercel Token 和项目名后即可校验或创建项目。",
                recentDeployments = history,
            )
        }
        val project = vercelGatewayFactory(settings.deploymentAccessToken).getProject(
            projectName = settings.deploymentProjectName,
            teamId = settings.deploymentTeamId.ifBlank { null },
        )
        return DeploymentCenterSnapshot(
            provider = DeploymentPlatform.Vercel,
            repositoryName = repositoryName,
            projectName = project.name,
            projectId = project.id,
            productionDomain = project.latestDomains.firstOrNull().orEmpty(),
            previewDomain = project.targets.preview.alias.firstOrNull().orEmpty(),
            customDomain = settings.deploymentCustomDomain,
            customDomainStatus = settings.deploymentCustomDomainStatus,
            consoleUrl = "https://vercel.com/dashboard",
            recentDeployments = history,
        )
    }

    private suspend fun loadCloudflareSnapshot(
        settings: GitHubSettings,
        repositoryName: String,
        history: List<DeploymentRecord>,
    ): DeploymentCenterSnapshot {
        if (settings.deploymentAccessToken.isBlank() ||
            settings.deploymentAccountId.isBlank() ||
            settings.deploymentProjectName.isBlank()
        ) {
            return DeploymentCenterSnapshot(
                provider = DeploymentPlatform.CloudflarePages,
                repositoryName = repositoryName,
                projectName = settings.deploymentProjectName,
                productionDomain = settings.deploymentProductionDomain,
                previewDomain = settings.deploymentPreviewDomain,
                customDomain = settings.deploymentCustomDomain,
                customDomainStatus = settings.deploymentCustomDomainStatus,
                setupHint = "填写 Cloudflare API Token、Account ID 和项目名后即可校验或创建项目。",
                recentDeployments = history,
            )
        }
        val project = cloudflareGatewayFactory(settings.deploymentAccessToken)
            .getProject(settings.deploymentAccountId, settings.deploymentProjectName)
            .result
        return DeploymentCenterSnapshot(
            provider = DeploymentPlatform.CloudflarePages,
            repositoryName = repositoryName,
            projectName = project.name,
            productionDomain = project.subdomain,
            previewDomain = project.domains.firstOrNull().orEmpty(),
            customDomain = settings.deploymentCustomDomain,
            customDomainStatus = settings.deploymentCustomDomainStatus,
            consoleUrl = "https://dash.cloudflare.com/",
            recentDeployments = history,
        )
    }

    private fun loadEdgeOneSnapshot(
        settings: GitHubSettings,
        repositoryName: String,
        history: List<DeploymentRecord>,
    ): DeploymentCenterSnapshot {
        val setupHint = when (settings.edgeOneExecutionMode) {
            EdgeOneExecutionMode.GitHubActions ->
                "推荐通过 GitHub Actions 执行 EdgeOne CLI，并在仓库 Secrets 中配置 EDGEONE_API_TOKEN。"

            EdgeOneExecutionMode.LimitedLocalCli ->
                "本地 CLI 运行仍是受限实验能力，请优先使用 GitHub Actions。"
        }
        return DeploymentCenterSnapshot(
            provider = DeploymentPlatform.EdgeOnePages,
            repositoryName = repositoryName,
            projectName = settings.deploymentProjectName.ifBlank { settings.repo },
            productionDomain = settings.deploymentProductionDomain,
            previewDomain = settings.deploymentPreviewDomain,
            customDomain = settings.deploymentCustomDomain,
            customDomainStatus = settings.deploymentCustomDomainStatus,
            consoleUrl = "https://console.tencentcloud.com/edgeone/pages",
            setupHint = setupHint,
            recentDeployments = history,
        )
    }

    private suspend fun createVercelProject(settings: GitHubSettings): DeploymentActionResult {
        require(settings.deploymentAccessToken.isNotBlank()) { "请先填写 Vercel Token" }
        require(settings.deploymentProjectName.isNotBlank()) { "请先填写 Vercel 项目名" }
        val gateway = vercelGatewayFactory(settings.deploymentAccessToken)
        gateway.createProject(
            request = VercelCreateProjectRequest(
                name = settings.deploymentProjectName,
                gitRepository = VercelGitRepository(
                    repo = settings.repo,
                    productionBranch = settings.branch,
                ),
                buildCommand = settings.deploymentBuildCommand,
                outputDirectory = settings.deploymentOutputDirectory,
            ),
            teamId = settings.deploymentTeamId.ifBlank { null },
        )
        return DeploymentActionResult(true, "已向 Vercel 提交项目创建请求")
    }

    private suspend fun createCloudflareProject(settings: GitHubSettings): DeploymentActionResult {
        require(settings.deploymentAccessToken.isNotBlank()) { "请先填写 Cloudflare Token" }
        require(settings.deploymentAccountId.isNotBlank()) { "请先填写 Cloudflare Account ID" }
        require(settings.deploymentProjectName.isNotBlank()) { "请先填写 Cloudflare 项目名" }
        val gateway = cloudflareGatewayFactory(settings.deploymentAccessToken)
        gateway.createProject(
            accountId = settings.deploymentAccountId,
            request = CloudflareCreateProjectRequest(
                name = settings.deploymentProjectName,
                productionBranch = settings.branch,
                buildConfig = CloudflareBuildConfig(
                    buildCommand = settings.deploymentBuildCommand,
                    destinationDir = settings.deploymentOutputDirectory,
                ),
                repository = CloudflareGitRepository(repo = settings.repo),
            ),
        )
        return DeploymentActionResult(true, "已向 Cloudflare Pages 提交项目创建请求")
    }

    private suspend fun createEdgeOneWorkflow(settings: GitHubSettings): DeploymentActionResult {
        val workflow = edgeOneWorkflowManager.buildWorkflow(settings)
        workspaceRepository.saveFile(
            settings = settings,
            path = workflow.path,
            content = workflow.content,
            commitMessage = "chore(deploy): add EdgeOne Pages workflow",
        )
        return DeploymentActionResult(true, workflow.summary)
    }
}
