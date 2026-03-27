package com.blogmd.mizukiwriter.data.github

import com.blogmd.mizukiwriter.data.media.AssetStorage
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.io.path.createTempDirectory

class GitHubPublisherTest {

    @Test
    fun `publish falls back to master branch and updates git ref`() = runTest {
        val root = createTempDirectory("publisher-test").toFile()
        val assetStorage = AssetStorage(root)
        val gateway = RecordingGitHubGateway().apply {
            contentShas["src/content/posts/hello-world/index.md"] = "sha-index"
        }
        val publisher = GitHubPublisher(
            assetStorage = assetStorage,
            gatewayFactory = { gateway },
        )
        val draft = DraftPost(
            id = 1L,
            title = "Hello World",
            description = "Desc",
            published = "2025-01-20",
            slug = "hello-world",
            body = "Body",
        )
        val settings = GitHubSettings(
            owner = "demo",
            repo = "blog",
            branch = "",
            postsBasePath = "src/content/posts",
            personalAccessToken = "token",
        )

        val result = publisher.publish(draft, settings, overwrite = true)

        assertThat(result).isEqualTo(GitHubPublishResult.Success("hello-world"))
        assertThat(gateway.requestedRefs).contains("master")
        assertThat(gateway.createdBlobs).hasSize(1)
        assertThat(gateway.updatedRefs.single().branch).isEqualTo("master")
        assertThat(gateway.updatedRefs.single().body.sha).isEqualTo("commit-new")
    }

    @Test
    fun `publish returns conflict when article already exists and overwrite is false`() = runTest {
        val root = createTempDirectory("publisher-test").toFile()
        val assetStorage = AssetStorage(root)
        val gateway = RecordingGitHubGateway().apply {
            contentShas["src/content/posts/hello-world/index.md"] = "sha-index"
        }
        val publisher = GitHubPublisher(
            assetStorage = assetStorage,
            gatewayFactory = { gateway },
        )
        val draft = DraftPost(
            id = 1L,
            title = "Hello World",
            description = "Desc",
            published = "2025-01-20",
            slug = "hello-world",
        )
        val settings = GitHubSettings(
            owner = "demo",
            repo = "blog",
            branch = "master",
            postsBasePath = "src/content/posts",
            personalAccessToken = "token",
        )

        val result = publisher.publish(draft, settings, overwrite = false)

        assertThat(result).isEqualTo(
            GitHubPublishResult.Conflict("hello-world", "src/content/posts/hello-world/index.md"),
        )
        assertThat(gateway.updatedRefs).isEmpty()
    }

    @Test
    fun `publish returns GitHub error details from response body`() = runTest {
        val root = createTempDirectory("publisher-test").toFile()
        val assetStorage = AssetStorage(root)
        val gateway = object : GitHubGateway {
            override suspend fun getContent(
                owner: String,
                repo: String,
                path: String,
                ref: String,
            ): GitHubContentResponse = GitHubContentResponse()

            override suspend fun deleteContent(
                owner: String,
                repo: String,
                path: String,
                body: GitHubDeleteRequest,
            ): GitHubContentResponse = error("Not used")

            override suspend fun getBranchRef(owner: String, repo: String, branch: String): GitHubRefResponse =
                GitHubRefResponse(obj = GitHubShaObject("commit-head"))

            override suspend fun getCommit(owner: String, repo: String, commitSha: String): GitHubCommitResponse =
                GitHubCommitResponse(sha = "commit-head", tree = GitHubShaObject("tree-base"))

            override suspend fun getTree(
                owner: String,
                repo: String,
                treeSha: String,
                recursive: Boolean,
            ): GitHubTreeResponse = error("Not used")

            override suspend fun createBlob(owner: String, repo: String, body: GitHubBlobRequest): GitHubBlobResponse =
                throw HttpException(
                    Response.error<GitHubBlobResponse>(
                        422,
                        """{"message":"Validation Failed","errors":[{"message":"invalid blob content"}]}"""
                            .toResponseBody("application/json".toMediaType()),
                    ),
                )

            override suspend fun createTree(owner: String, repo: String, body: GitHubCreateTreeRequest): GitHubTreeResponse =
                error("Not used")

            override suspend fun createCommit(owner: String, repo: String, body: GitHubCreateCommitRequest): GitHubCommitResponse =
                error("Not used")

            override suspend fun updateBranchRef(
                owner: String,
                repo: String,
                branch: String,
                body: GitHubUpdateRefRequest,
            ): GitHubRefResponse = error("Not used")
        }
        val publisher = GitHubPublisher(
            assetStorage = assetStorage,
            gatewayFactory = { gateway },
        )
        val draft = DraftPost(
            id = 1L,
            title = "Hello World",
            description = "Desc",
            published = "2025-01-20",
            slug = "hello-world",
        )
        val settings = GitHubSettings(
            owner = "demo",
            repo = "blog",
            branch = "master",
            postsBasePath = "src/content/posts",
            personalAccessToken = "token",
        )

        val result = publisher.publish(draft, settings, overwrite = true)

        assertThat(result).isEqualTo(
            GitHubPublishResult.Failure("GitHub 请求失败: 422 - Validation Failed；invalid blob content"),
        )
    }

    @Test
    fun `deletes remote article root and uploaded assets`() = runTest {
        val root = createTempDirectory("publisher-test").toFile()
        val assetStorage = AssetStorage(root)
        val draftId = 42L

        val gateway = RecordingGitHubGateway().apply {
            treeEntries += GitHubTreeNode(path = "src/content/posts/hello-world/index.md", sha = "sha-index")
            treeEntries += GitHubTreeNode(path = "src/content/posts/hello-world/cover.png", sha = "sha-cover")
            treeEntries += GitHubTreeNode(path = "src/content/posts/hello-world/diagram.jpg", sha = "sha-diagram")
        }
        val publisher = GitHubPublisher(
            assetStorage = assetStorage,
            gatewayFactory = { gateway },
        )
        val draft = DraftPost(
            id = draftId,
            title = "Hello",
            description = "World",
            published = "2025-01-20",
            slug = "hello-world",
        )
        val settings = GitHubSettings(
            owner = "demo",
            repo = "blog",
            branch = "main",
            postsBasePath = "src/content/posts",
            personalAccessToken = "token",
        )

        val result = publisher.deleteRemoteArticle(draft, settings)

        assertThat(result).isEqualTo(GitHubDeleteResult.Success("hello-world"))
        assertThat(gateway.requestedRefs).contains("main")
        assertThat(gateway.createdTrees.single().tree.map { it.path }).containsExactly(
            "src/content/posts/hello-world/cover.png",
            "src/content/posts/hello-world/diagram.jpg",
            "src/content/posts/hello-world/index.md",
        )
        assertThat(gateway.createdTrees.single().tree.all { it.sha == null }).isTrue()
        assertThat(gateway.updatedRefs.single().branch).isEqualTo("main")
    }

    @Test
    fun `delete remote article fails when remote article is missing`() = runTest {
        val root = createTempDirectory("publisher-test").toFile()
        val assetStorage = AssetStorage(root)
        val gateway = RecordingGitHubGateway()
        val publisher = GitHubPublisher(
            assetStorage = assetStorage,
            gatewayFactory = { gateway },
        )
        val draft = DraftPost(
            id = 7L,
            title = "Missing",
            description = "Desc",
            published = "2025-01-20",
            slug = "missing-article",
        )
        val settings = GitHubSettings(
            owner = "demo",
            repo = "blog",
            branch = "main",
            postsBasePath = "src/content/posts",
            personalAccessToken = "token",
        )

        val result = publisher.deleteRemoteArticle(draft, settings)

        assertThat(result).isEqualTo(
            GitHubDeleteResult.Failure("未找到 GitHub 远程文章：src/content/posts/missing-article"),
        )
        assertThat(gateway.createdTrees).isEmpty()
        assertThat(gateway.updatedRefs).isEmpty()
    }

    private class RecordingGitHubGateway : GitHubGateway {
        val contentShas = mutableMapOf<String, String>()
        val deletedPaths = mutableListOf<String>()
        val requestedRefs = mutableListOf<String>()
        val createdBlobs = mutableListOf<GitHubBlobRequest>()
        val createdTrees = mutableListOf<GitHubCreateTreeRequest>()
        val createdCommits = mutableListOf<GitHubCreateCommitRequest>()
        val updatedRefs = mutableListOf<RefUpdateCall>()
        val treeEntries = mutableListOf<GitHubTreeNode>()

        override suspend fun getContent(owner: String, repo: String, path: String, ref: String): GitHubContentResponse {
            requestedRefs += ref
            return GitHubContentResponse(sha = contentShas[path] ?: "sha-$path")
        }

        override suspend fun deleteContent(owner: String, repo: String, path: String, body: GitHubDeleteRequest): GitHubContentResponse {
            deletedPaths += path
            return GitHubContentResponse(sha = body.sha)
        }

        override suspend fun getBranchRef(owner: String, repo: String, branch: String): GitHubRefResponse {
            requestedRefs += branch
            return GitHubRefResponse(obj = GitHubShaObject("commit-head"))
        }

        override suspend fun getCommit(owner: String, repo: String, commitSha: String): GitHubCommitResponse =
            GitHubCommitResponse(sha = commitSha, tree = GitHubShaObject("tree-base"))

        override suspend fun getTree(owner: String, repo: String, treeSha: String, recursive: Boolean): GitHubTreeResponse {
            return GitHubTreeResponse(sha = treeSha, tree = treeEntries.toList())
        }

        override suspend fun createBlob(owner: String, repo: String, body: GitHubBlobRequest): GitHubBlobResponse {
            createdBlobs += body
            return GitHubBlobResponse(sha = "blob-${createdBlobs.size}")
        }

        override suspend fun createTree(owner: String, repo: String, body: GitHubCreateTreeRequest): GitHubTreeResponse {
            createdTrees += body
            return GitHubTreeResponse(sha = "tree-new", tree = body.tree.map { GitHubTreeNode(path = it.path, mode = it.mode, type = it.type, sha = it.sha) })
        }

        override suspend fun createCommit(owner: String, repo: String, body: GitHubCreateCommitRequest): GitHubCommitResponse {
            createdCommits += body
            return GitHubCommitResponse(sha = "commit-new", tree = GitHubShaObject(body.tree))
        }

        override suspend fun updateBranchRef(
            owner: String,
            repo: String,
            branch: String,
            body: GitHubUpdateRefRequest,
        ): GitHubRefResponse {
            updatedRefs += RefUpdateCall(branch = branch, body = body)
            return GitHubRefResponse(refName = "refs/heads/$branch", obj = GitHubShaObject(body.sha))
        }
    }

    private data class RefUpdateCall(
        val branch: String,
        val body: GitHubUpdateRefRequest,
    )
}
