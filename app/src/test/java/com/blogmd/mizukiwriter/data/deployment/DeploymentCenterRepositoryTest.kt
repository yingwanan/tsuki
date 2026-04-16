package com.blogmd.mizukiwriter.data.deployment

import com.blogmd.mizukiwriter.data.github.DeploymentRecord
import com.blogmd.mizukiwriter.data.github.GitHubContentResponse
import com.blogmd.mizukiwriter.data.github.GitHubRepositoryResponse
import com.blogmd.mizukiwriter.data.github.GitHubWorkspaceRepositoryContract
import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform
import com.blogmd.mizukiwriter.data.settings.EdgeOneExecutionMode
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.domain.SiteConfigSnapshot
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeploymentCenterRepositoryTest {

    @Test
    fun `loadSnapshot returns provider domains and deployment history for vercel`() = runTest {
        val repository = DeploymentCenterRepository(
            workspaceRepository = FakeWorkspaceRepository(),
            vercelGatewayFactory = { FakeVercelGateway() },
            cloudflareGatewayFactory = { error("Not used") },
            edgeOneWorkflowManager = FakeEdgeOneWorkflowManager(),
        )

        val snapshot = repository.loadSnapshot(
            GitHubSettings(
                owner = "demo",
                repo = "blog",
                personalAccessToken = "github-token",
                deploymentPlatform = DeploymentPlatform.Vercel,
                deploymentAccessToken = "vercel-token",
                deploymentProjectName = "blog-console",
            ),
        )

        assertThat(snapshot.provider).isEqualTo(DeploymentPlatform.Vercel)
        assertThat(snapshot.projectName).isEqualTo("blog-console")
        assertThat(snapshot.productionDomain).isEqualTo("blog-console.vercel.app")
        assertThat(snapshot.recentDeployments).hasSize(1)
    }

    @Test
    fun `createProject delegates git-linked project creation to vercel`() = runTest {
        val gateway = FakeVercelGateway()
        val repository = DeploymentCenterRepository(
            workspaceRepository = FakeWorkspaceRepository(),
            vercelGatewayFactory = { gateway },
            cloudflareGatewayFactory = { error("Not used") },
            edgeOneWorkflowManager = FakeEdgeOneWorkflowManager(),
        )

        val result = repository.createProject(
            GitHubSettings(
                owner = "demo",
                repo = "blog",
                branch = "main",
                personalAccessToken = "github-token",
                deploymentPlatform = DeploymentPlatform.Vercel,
                deploymentAccessToken = "vercel-token",
                deploymentProjectName = "blog-console",
                deploymentBuildCommand = "pnpm build",
                deploymentOutputDirectory = "dist",
            ),
        )

        assertThat(result.success).isTrue()
        assertThat(gateway.createdProject?.name).isEqualTo("blog-console")
        assertThat(gateway.createdProject?.gitRepository?.repo).isEqualTo("blog")
        assertThat(gateway.createdProject?.gitRepository?.productionBranch).isEqualTo("main")
    }

    @Test
    fun `createProject delegates build configuration to cloudflare`() = runTest {
        val gateway = FakeCloudflareGateway()
        val repository = DeploymentCenterRepository(
            workspaceRepository = FakeWorkspaceRepository(),
            vercelGatewayFactory = { error("Not used") },
            cloudflareGatewayFactory = { gateway },
            edgeOneWorkflowManager = FakeEdgeOneWorkflowManager(),
        )

        val result = repository.createProject(
            GitHubSettings(
                owner = "demo",
                repo = "blog",
                branch = "master",
                personalAccessToken = "github-token",
                deploymentPlatform = DeploymentPlatform.CloudflarePages,
                deploymentAccessToken = "cf-token",
                deploymentAccountId = "account-1",
                deploymentProjectName = "blog-console",
                deploymentBuildCommand = "pnpm build",
                deploymentOutputDirectory = "dist",
            ),
        )

        assertThat(result.success).isTrue()
        assertThat(gateway.createdProject?.name).isEqualTo("blog-console")
        assertThat(gateway.createdProject?.productionBranch).isEqualTo("master")
        assertThat(gateway.createdProject?.buildConfig?.destinationDir).isEqualTo("dist")
    }

    @Test
    fun `createProject writes edgeone workflow when platform is edgeone`() = runTest {
        val workflowManager = FakeEdgeOneWorkflowManager()
        val workspaceRepository = FakeWorkspaceRepository()
        val repository = DeploymentCenterRepository(
            workspaceRepository = workspaceRepository,
            vercelGatewayFactory = { error("Not used") },
            cloudflareGatewayFactory = { error("Not used") },
            edgeOneWorkflowManager = workflowManager,
        )

        val result = repository.createProject(
            GitHubSettings(
                owner = "demo",
                repo = "blog",
                branch = "master",
                personalAccessToken = "github-token",
                deploymentPlatform = DeploymentPlatform.EdgeOnePages,
                deploymentAccessToken = "edgeone-token",
                deploymentProjectName = "blog-console",
                edgeOneExecutionMode = EdgeOneExecutionMode.GitHubActions,
            ),
        )

        assertThat(result.success).isTrue()
        assertThat(workspaceRepository.savedPath).isEqualTo(".github/workflows/deploy-edgeone-pages.yml")
        assertThat(workspaceRepository.savedContent).contains("edgeone pages deploy")
        assertThat(workflowManager.lastSettings?.deploymentProjectName).isEqualTo("blog-console")
    }

    private class FakeWorkspaceRepository : GitHubWorkspaceRepositoryContract {
        var savedPath = ""
        var savedContent = ""

        override suspend fun listRemoteContent(settings: GitHubSettings) = emptyList<com.blogmd.mizukiwriter.data.github.RemoteContentItem>()

        override suspend fun loadFile(settings: GitHubSettings, path: String) = error("Not used")

        override suspend fun saveFile(
            settings: GitHubSettings,
            path: String,
            content: String,
            commitMessage: String,
        ): GitHubContentResponse {
            savedPath = path
            savedContent = content
            return GitHubContentResponse(path = path, sha = "saved-sha")
        }

        override suspend fun deleteFile(settings: GitHubSettings, path: String, commitMessage: String) = error("Not used")

        override suspend fun loadSiteConfig(settings: GitHubSettings) = SiteConfigSnapshot() to ""

        override suspend fun saveSiteConfig(settings: GitHubSettings, snapshot: SiteConfigSnapshot, source: String) =
            error("Not used")

        override suspend fun listDeployments(settings: GitHubSettings) = listOf(
            DeploymentRecord(
                id = 1L,
                name = "Deploy to Pages",
                status = "completed",
                conclusion = "success",
                htmlUrl = "https://github.com/demo/blog/actions/runs/1",
                branch = settings.branch,
                updatedAt = "2026-04-05T10:00:00Z",
            ),
        )

        override suspend fun loadRepository(settings: GitHubSettings) =
            GitHubRepositoryResponse(fullName = "${settings.owner}/${settings.repo}", defaultBranch = settings.branch)
    }

    private class FakeVercelGateway : VercelGateway {
        var createdProject: VercelCreateProjectRequest? = null

        override suspend fun getProject(projectName: String, teamId: String?): VercelProject =
            VercelProject(
                id = "prj_1",
                name = projectName,
                framework = "astro",
                latestDomains = listOf("blog-console.vercel.app"),
                targets = VercelProjectTargets(
                    production = VercelProjectTarget(alias = emptyList()),
                    preview = VercelProjectTarget(alias = listOf("preview-blog-console.vercel.app")),
                ),
            )

        override suspend fun createProject(request: VercelCreateProjectRequest, teamId: String?): VercelProject {
            createdProject = request
            return getProject(request.name, teamId)
        }

        override suspend fun addDomain(projectName: String, request: VercelAddDomainRequest, teamId: String?) =
            VercelDomainResponse(name = request.name, apexName = request.name)
    }

    private class FakeCloudflareGateway : CloudflarePagesGateway {
        var createdProject: CloudflareCreateProjectRequest? = null

        override suspend fun getProject(accountId: String, projectName: String): CloudflareEnvelope<CloudflareProject> =
            CloudflareEnvelope(
                result = CloudflareProject(
                    name = projectName,
                    subdomain = "$projectName.pages.dev",
                    domains = listOf("preview.$projectName.pages.dev"),
                ),
            )

        override suspend fun createProject(
            accountId: String,
            request: CloudflareCreateProjectRequest,
        ): CloudflareEnvelope<CloudflareProject> {
            createdProject = request
            return getProject(accountId, request.name)
        }

        override suspend fun addDomain(
            accountId: String,
            projectName: String,
            request: CloudflareDomainRequest,
        ) = CloudflareEnvelope(result = CloudflareDomainResult(name = request.name, status = "pending"))
    }

    private class FakeEdgeOneWorkflowManager : EdgeOneWorkflowManager {
        var lastSettings: GitHubSettings? = null

        override fun buildWorkflow(settings: GitHubSettings): EdgeOneWorkflowPlan {
            lastSettings = settings
            return EdgeOneWorkflowPlan(
                path = ".github/workflows/deploy-edgeone-pages.yml",
                content = "name: Deploy\nrun: npx edgeone pages deploy -n ${settings.deploymentProjectName}",
                summary = "写入 EdgeOne 工作流",
            )
        }
    }
}
