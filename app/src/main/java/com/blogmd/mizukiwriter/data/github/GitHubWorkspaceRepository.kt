package com.blogmd.mizukiwriter.data.github

import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.domain.GitBranchTargetResolver
import com.blogmd.mizukiwriter.domain.MizukiConfigParser
import com.blogmd.mizukiwriter.domain.SiteConfigSnapshot
import retrofit2.HttpException
import java.util.Base64

data class RemoteContentItem(
    val path: String,
    val sha: String,
    val type: RemoteContentType,
)

enum class RemoteContentType {
    Post,
    Page,
}

data class RemoteFileDocument(
    val path: String,
    val sha: String,
    val content: String,
    val branch: String,
)

data class DeploymentRecord(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val htmlUrl: String,
    val branch: String,
    val updatedAt: String,
)

class GitHubWorkspaceRepository(
    private val gatewayFactory: (String) -> GitHubGateway = { token -> createGateway(token) },
) : GitHubWorkspaceRepositoryContract {
    override suspend fun listRemoteContent(settings: GitHubSettings): List<RemoteContentItem> {
        val gateway = gatewayFactory(settings.personalAccessToken)
        val branch = resolveTargetBranch(settings)
        val headRef = gateway.getBranchRef(settings.owner, settings.repo, branch)
        val headCommit = gateway.getCommit(settings.owner, settings.repo, headRef.obj.sha)
        val tree = gateway.getTree(settings.owner, settings.repo, headCommit.tree.sha)

        return tree.tree.mapNotNull { node ->
            if (node.type != "blob" || node.sha == null) return@mapNotNull null
            when {
                node.path.startsWith("${settings.postsBasePath.trim('/')}/") && node.path.endsWith(".md") ->
                    RemoteContentItem(node.path, node.sha, RemoteContentType.Post)

                node.path.startsWith("${settings.pagesBasePath.trim('/')}/") && node.path.endsWith(".md") ->
                    RemoteContentItem(node.path, node.sha, RemoteContentType.Page)

                else -> null
            }
        }
    }

    override suspend fun loadFile(settings: GitHubSettings, path: String): RemoteFileDocument {
        val gateway = gatewayFactory(settings.personalAccessToken)
        val branch = resolveTargetBranch(settings)
        val metadata = gateway.getContent(settings.owner, settings.repo, path, branch)
        return RemoteFileDocument(
            path = path,
            sha = metadata.sha,
            content = metadata.decodeContent(),
            branch = branch,
        )
    }

    override suspend fun saveFile(
        settings: GitHubSettings,
        path: String,
        content: String,
        commitMessage: String,
    ): GitHubContentResponse {
        val gateway = gatewayFactory(settings.personalAccessToken)
        val branch = ensureTargetBranch(settings, gateway)
        val existingSha = runCatching {
            gateway.getContent(settings.owner, settings.repo, path, branch).sha
        }.getOrNull()
        return gateway.putContent(
            settings.owner,
            settings.repo,
            path,
            GitHubContentRequest(
                message = commitMessage,
                content = content.encodeToByteArray().toBase64(),
                branch = branch,
                sha = existingSha,
            ),
        )
    }

    override suspend fun deleteFile(
        settings: GitHubSettings,
        path: String,
        commitMessage: String,
    ): GitHubContentResponse {
        val gateway = gatewayFactory(settings.personalAccessToken)
        val branch = ensureTargetBranch(settings, gateway)
        val existing = gateway.getContent(settings.owner, settings.repo, path, branch)
        return gateway.deleteContent(
            settings.owner,
            settings.repo,
            path,
            GitHubDeleteRequest(
                message = commitMessage,
                branch = branch,
                sha = existing.sha,
            ),
        )
    }

    override suspend fun loadSiteConfig(settings: GitHubSettings): Pair<SiteConfigSnapshot, String> {
        val document = loadFile(settings, settings.configPath)
        return MizukiConfigParser.parse(document.content) to document.content
    }

    override suspend fun saveSiteConfig(
        settings: GitHubSettings,
        snapshot: SiteConfigSnapshot,
        source: String,
    ): GitHubContentResponse = saveFile(
        settings = settings,
        path = settings.configPath,
        content = MizukiConfigParser.update(source, snapshot),
        commitMessage = "chore(config): update site settings",
    )

    override suspend fun listDeployments(settings: GitHubSettings): List<DeploymentRecord> {
        val gateway = gatewayFactory(settings.personalAccessToken)
        val runs = gateway.listWorkflowRuns(
            owner = settings.owner,
            repo = settings.repo,
            branch = settings.branch.ifBlank { null },
        )
        return runs.workflowRuns.map { run ->
            DeploymentRecord(
                id = run.id,
                name = run.name,
                status = run.status,
                conclusion = run.conclusion,
                htmlUrl = run.htmlUrl,
                branch = run.headBranch,
                updatedAt = run.updatedAt,
            )
        }
    }

    override suspend fun loadRepository(settings: GitHubSettings): GitHubRepositoryResponse {
        val gateway = gatewayFactory(settings.personalAccessToken)
        return gateway.getRepository(settings.owner, settings.repo)
    }

    private suspend fun ensureTargetBranch(settings: GitHubSettings, gateway: GitHubGateway): String {
        val branchTarget = GitBranchTargetResolver.resolve(
            settings = settings,
            generatedBranchName = "tsuki/update/blog-console-suite",
        )
        return try {
            gateway.getBranchRef(settings.owner, settings.repo, branchTarget.targetBranch)
            branchTarget.targetBranch
        } catch (error: HttpException) {
            if (!branchTarget.requiresBranchCreation || error.code() != 404) throw error
            val baseRef = gateway.getBranchRef(settings.owner, settings.repo, branchTarget.baseBranch)
            gateway.createBranchRef(
                settings.owner,
                settings.repo,
                GitHubCreateRefRequest(
                    ref = "refs/heads/${branchTarget.targetBranch}",
                    sha = baseRef.obj.sha,
                ),
            )
            branchTarget.targetBranch
        }
    }

    private fun resolveTargetBranch(settings: GitHubSettings): String = GitBranchTargetResolver.resolve(
        settings = settings,
        generatedBranchName = "tsuki/update/blog-console-suite",
    ).targetBranch
}

interface GitHubWorkspaceRepositoryContract {
    suspend fun listRemoteContent(settings: GitHubSettings): List<RemoteContentItem>
    suspend fun loadFile(settings: GitHubSettings, path: String): RemoteFileDocument
    suspend fun saveFile(
        settings: GitHubSettings,
        path: String,
        content: String,
        commitMessage: String,
    ): GitHubContentResponse

    suspend fun deleteFile(
        settings: GitHubSettings,
        path: String,
        commitMessage: String,
    ): GitHubContentResponse

    suspend fun loadSiteConfig(settings: GitHubSettings): Pair<SiteConfigSnapshot, String>
    suspend fun saveSiteConfig(
        settings: GitHubSettings,
        snapshot: SiteConfigSnapshot,
        source: String,
    ): GitHubContentResponse

    suspend fun listDeployments(settings: GitHubSettings): List<DeploymentRecord>
    suspend fun loadRepository(settings: GitHubSettings): GitHubRepositoryResponse
}

private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

private fun GitHubContentDocumentResponse.decodeContent(): String {
    if (content.isBlank()) return ""
    if (encoding != "base64") return content
    return String(Base64.getMimeDecoder().decode(content))
}

private fun GitHubBlobContentResponse.decodeContent(): String {
    if (encoding != "base64") return content
    return String(Base64.getMimeDecoder().decode(content))
}
