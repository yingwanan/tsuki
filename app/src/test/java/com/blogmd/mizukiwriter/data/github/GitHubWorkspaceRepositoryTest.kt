package com.blogmd.mizukiwriter.data.github

import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.data.settings.WorkspaceMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class GitHubWorkspaceRepositoryTest {

    @Test
    fun `listRemoteContent returns markdown files from configured roots`() = runTest {
        val gateway = FakeWorkspaceGateway().apply {
            repository = GitHubRepositoryResponse(defaultBranch = "master")
            tree = GitHubTreeResponse(
                sha = "tree-root",
                tree = listOf(
                    GitHubTreeNode(path = "src/content/posts/hello/index.md", type = "blob", sha = "post-sha"),
                    GitHubTreeNode(path = "src/content/spec/about.md", type = "blob", sha = "page-sha"),
                    GitHubTreeNode(path = "public/assets/logo.png", type = "blob", sha = "image-sha"),
                    GitHubTreeNode(path = "README.md", type = "blob", sha = "ignored"),
                ),
            )
        }
        val repository = GitHubWorkspaceRepository { gateway }

        val items = repository.listRemoteContent(
            GitHubSettings(owner = "demo", repo = "blog", personalAccessToken = "token"),
        )

        assertThat(items.map { it.path }).containsExactly(
            "src/content/posts/hello/index.md",
            "src/content/spec/about.md",
        )
    }

    @Test
    fun `saveFile commits directly to configured branch in direct commit mode`() = runTest {
        val gateway = FakeWorkspaceGateway().apply {
            repository = GitHubRepositoryResponse(defaultBranch = "main")
            content = GitHubContentDocumentResponse(sha = "file-sha", path = "src/config.ts")
        }
        val repository = GitHubWorkspaceRepository { gateway }

        repository.saveFile(
            settings = GitHubSettings(
                owner = "demo",
                repo = "blog",
                branch = "main",
                workspaceMode = WorkspaceMode.DirectCommit,
                personalAccessToken = "token",
            ),
            path = "src/config.ts",
            content = "updated",
            commitMessage = "chore: update config",
        )

        assertThat(gateway.createdRefs).isEmpty()
        assertThat(gateway.putRequests.single().branch).isEqualTo("main")
    }

    @Test
    fun `saveFile creates working branch when missing and commits to update branch`() = runTest {
        val gateway = FakeWorkspaceGateway().apply {
            repository = GitHubRepositoryResponse(defaultBranch = "master")
            content = GitHubContentDocumentResponse(sha = "file-sha", path = "src/config.ts")
            missingRefs += "tsuki/update/blog-console-suite"
        }
        val repository = GitHubWorkspaceRepository { gateway }

        repository.saveFile(
            settings = GitHubSettings(
                owner = "demo",
                repo = "blog",
                branch = "master",
                workspaceMode = WorkspaceMode.WorkingBranch,
                updateBranch = "tsuki/update/blog-console-suite",
                personalAccessToken = "token",
            ),
            path = "src/config.ts",
            content = "updated",
            commitMessage = "chore: update config",
        )

        assertThat(gateway.createdRefs.single().ref).isEqualTo("refs/heads/tsuki/update/blog-console-suite")
        assertThat(gateway.putRequests.single().branch).isEqualTo("tsuki/update/blog-console-suite")
    }

    private class FakeWorkspaceGateway : GitHubGateway {
        var repository = GitHubRepositoryResponse(defaultBranch = "master")
        var tree = GitHubTreeResponse()
        var content = GitHubContentDocumentResponse()
        val missingRefs = mutableSetOf<String>()
        val createdRefs = mutableListOf<GitHubCreateRefRequest>()
        val putRequests = mutableListOf<GitHubContentRequest>()

        override suspend fun getContent(
            owner: String,
            repo: String,
            path: String,
            ref: String,
        ): GitHubContentDocumentResponse = content

        override suspend fun putContent(
            owner: String,
            repo: String,
            path: String,
            body: GitHubContentRequest,
        ): GitHubContentResponse {
            putRequests += body
            return GitHubContentResponse(
                sha = "saved-sha",
                content = GitHubContentDescriptor(path = path, sha = "saved-sha"),
            )
        }

        override suspend fun deleteContent(
            owner: String,
            repo: String,
            path: String,
            body: GitHubDeleteRequest,
        ): GitHubContentResponse = GitHubContentResponse()

        override suspend fun getBranchRef(owner: String, repo: String, branch: String): GitHubRefResponse {
            if (missingRefs.contains(branch)) {
                throw HttpException(Response.error<GitHubRefResponse>(404, "{}".toResponseBody()))
            }
            return GitHubRefResponse(obj = GitHubShaObject("head-sha"))
        }

        override suspend fun getCommit(owner: String, repo: String, commitSha: String): GitHubCommitResponse =
            GitHubCommitResponse(sha = "head-sha", tree = GitHubShaObject("tree-sha"))

        override suspend fun getTree(
            owner: String,
            repo: String,
            treeSha: String,
            recursive: Boolean,
        ): GitHubTreeResponse = tree

        override suspend fun createBlob(owner: String, repo: String, body: GitHubBlobRequest): GitHubBlobResponse =
            GitHubBlobResponse("blob-sha")

        override suspend fun getBlob(owner: String, repo: String, fileSha: String): GitHubBlobContentResponse =
            GitHubBlobContentResponse(sha = fileSha, content = "", encoding = "base64")

        override suspend fun createTree(owner: String, repo: String, body: GitHubCreateTreeRequest): GitHubTreeResponse =
            GitHubTreeResponse(sha = "tree-sha")

        override suspend fun createCommit(owner: String, repo: String, body: GitHubCreateCommitRequest): GitHubCommitResponse =
            GitHubCommitResponse(sha = "commit-sha", tree = GitHubShaObject("tree-sha"))

        override suspend fun updateBranchRef(
            owner: String,
            repo: String,
            branch: String,
            body: GitHubUpdateRefRequest,
        ): GitHubRefResponse = GitHubRefResponse(obj = GitHubShaObject(body.sha))

        override suspend fun getRepository(owner: String, repo: String): GitHubRepositoryResponse = repository

        override suspend fun listWorkflowRuns(owner: String, repo: String, branch: String?): GitHubWorkflowRunsResponse =
            GitHubWorkflowRunsResponse()

        override suspend fun createBranchRef(
            owner: String,
            repo: String,
            body: GitHubCreateRefRequest,
        ): GitHubRefResponse {
            createdRefs += body
            return GitHubRefResponse(refName = body.ref, obj = GitHubShaObject(body.sha))
        }
    }
}
