package com.blogmd.mizukiwriter.data.github

import com.blogmd.mizukiwriter.data.media.AssetStorageContract
import com.blogmd.mizukiwriter.data.model.DraftPost
import com.blogmd.mizukiwriter.data.settings.GitHubSettings
import com.blogmd.mizukiwriter.domain.FrontmatterBuilder
import com.blogmd.mizukiwriter.domain.SlugGenerator
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.File
import java.time.LocalDateTime
import java.util.Base64

interface GitHubGateway {
    suspend fun getContent(owner: String, repo: String, path: String, ref: String): GitHubContentResponse
    suspend fun deleteContent(owner: String, repo: String, path: String, body: GitHubDeleteRequest): GitHubContentResponse
    suspend fun getBranchRef(owner: String, repo: String, branch: String): GitHubRefResponse
    suspend fun getCommit(owner: String, repo: String, commitSha: String): GitHubCommitResponse
    suspend fun createBlob(owner: String, repo: String, body: GitHubBlobRequest): GitHubBlobResponse
    suspend fun createTree(owner: String, repo: String, body: GitHubCreateTreeRequest): GitHubTreeResponse
    suspend fun createCommit(owner: String, repo: String, body: GitHubCreateCommitRequest): GitHubCommitResponse
    suspend fun updateBranchRef(owner: String, repo: String, branch: String, body: GitHubUpdateRefRequest): GitHubRefResponse
}

interface GitHubPublisherContract {
    suspend fun publish(
        draft: DraftPost,
        settings: GitHubSettings,
        overwrite: Boolean = false,
    ): GitHubPublishResult

    suspend fun deleteRemoteArticle(
        draft: DraftPost,
        settings: GitHubSettings,
    ): GitHubDeleteResult
}

class GitHubPublisher(
    private val assetStorage: AssetStorageContract,
    private val gatewayFactory: (String) -> GitHubGateway = { token -> createGateway(token) },
) : GitHubPublisherContract {
    override suspend fun publish(
        draft: DraftPost,
        settings: GitHubSettings,
        overwrite: Boolean,
    ): GitHubPublishResult {
        if (settings.owner.isBlank() || settings.repo.isBlank() || settings.personalAccessToken.isBlank()) {
            return GitHubPublishResult.Failure("请先在设置页填写 GitHub owner、repo 和 PAT")
        }

        val slug = draft.slug.ifBlank { SlugGenerator.fromTitle(draft.title, LocalDateTime.now()) }
        val articleRoot = "${settings.postsBasePath.trim('/')}/$slug"
        val markdown = buildMarkdown(draft.copy(slug = slug), settings)
        val api = gatewayFactory(settings.personalAccessToken)
        val branch = settings.branch.ifBlank { GitHubSettings().branch }
        val indexPath = "$articleRoot/index.md"

        return try {
            val existingSha = runCatching {
                api.getContent(settings.owner, settings.repo, indexPath, branch).sha
            }.getOrNull()
            if (existingSha != null && !overwrite) {
                return GitHubPublishResult.Conflict(slug, indexPath)
            }

            val headRef = api.getBranchRef(settings.owner, settings.repo, branch)
            val headCommitSha = headRef.obj.sha
            val headCommit = api.getCommit(settings.owner, settings.repo, headCommitSha)

            val treeEntries = buildList {
                add(
                    buildTreeEntry(
                        api = api,
                        owner = settings.owner,
                        repo = settings.repo,
                        path = indexPath,
                        content = markdown.encodeToByteArray().toBase64(),
                    ),
                )
                for (file in assetStorage.listAssetFiles(draft.id)) {
                    add(
                        buildTreeEntry(
                            api = api,
                            owner = settings.owner,
                            repo = settings.repo,
                            path = "$articleRoot/${file.name}",
                            content = file.readAsBase64(),
                        ),
                    )
                }
            }

            val tree = api.createTree(
                settings.owner,
                settings.repo,
                GitHubCreateTreeRequest(
                    baseTree = headCommit.tree.sha,
                    tree = treeEntries,
                ),
            )
            val commit = api.createCommit(
                settings.owner,
                settings.repo,
                GitHubCreateCommitRequest(
                    message = "feat(blog): publish $slug",
                    tree = tree.sha,
                    parents = listOf(headCommitSha),
                ),
            )
            api.updateBranchRef(
                settings.owner,
                settings.repo,
                branch,
                GitHubUpdateRefRequest(sha = commit.sha),
            )

            GitHubPublishResult.Success(slug)
        } catch (error: HttpException) {
            GitHubPublishResult.Failure(error.toReadableMessage("GitHub 请求失败"))
        } catch (error: Throwable) {
            GitHubPublishResult.Failure(error.message ?: "未知发布错误")
        }
    }

    override suspend fun deleteRemoteArticle(
        draft: DraftPost,
        settings: GitHubSettings,
    ): GitHubDeleteResult {
        if (settings.owner.isBlank() || settings.repo.isBlank() || settings.personalAccessToken.isBlank()) {
            return GitHubDeleteResult.Failure("请先在设置页填写 GitHub owner、repo 和 PAT")
        }

        val slug = draft.slug.ifBlank { SlugGenerator.fromTitle(draft.title, LocalDateTime.now()) }
        val articleRoot = "${settings.postsBasePath.trim('/')}/$slug"
        val branch = settings.branch.ifBlank { GitHubSettings().branch }
        val api = gatewayFactory(settings.personalAccessToken)
        val remotePaths = buildList {
            assetStorage.listAssetNames(draft.id).forEach { add("$articleRoot/$it") }
            add("$articleRoot/index.md")
        }

        return try {
            for (path in remotePaths) {
                val sha = runCatching {
                    api.getContent(settings.owner, settings.repo, path, branch).sha
                }.getOrNull() ?: continue

                api.deleteContent(
                    owner = settings.owner,
                    repo = settings.repo,
                    path = path,
                    body = GitHubDeleteRequest(
                        message = "chore(blog): delete $slug",
                        branch = branch,
                        sha = sha,
                    ),
                )
            }
            GitHubDeleteResult.Success(slug)
        } catch (error: HttpException) {
            GitHubDeleteResult.Failure(error.toReadableMessage("GitHub 删除失败"))
        } catch (error: Throwable) {
            GitHubDeleteResult.Failure(error.message ?: "未知删除错误")
        }
    }

    private fun buildMarkdown(draft: DraftPost, settings: GitHubSettings): String {
        val frontmatter = FrontmatterBuilder.build(
            draft = draft,
            defaultAuthor = settings.defaultAuthor,
            defaultLicenseName = settings.defaultLicenseName,
        )
        return buildString {
            append(frontmatter)
            append("\n\n")
            append(draft.body)
            append("\n")
        }
    }

    private suspend fun buildTreeEntry(
        api: GitHubGateway,
        owner: String,
        repo: String,
        path: String,
        content: String,
    ): GitHubTreeEntry {
        val blob = api.createBlob(owner, repo, GitHubBlobRequest(content = content))
        return GitHubTreeEntry(path = path, sha = blob.sha)
    }
}

internal val githubJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private fun createGateway(token: String): GitHubGateway {
    val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            chain.proceed(request)
        }
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttp)
        .addConverterFactory(githubJson.asConverterFactory("application/json".toMediaType()))
        .build()

    val api = retrofit.create(GitHubContentApi::class.java)
    return object : GitHubGateway {
        override suspend fun getContent(owner: String, repo: String, path: String, ref: String): GitHubContentResponse =
            api.getContent(owner, repo, path, ref)

        override suspend fun deleteContent(owner: String, repo: String, path: String, body: GitHubDeleteRequest): GitHubContentResponse =
            api.deleteContent(owner, repo, path, body)

        override suspend fun getBranchRef(owner: String, repo: String, branch: String): GitHubRefResponse =
            api.getBranchRef(owner, repo, branch)

        override suspend fun getCommit(owner: String, repo: String, commitSha: String): GitHubCommitResponse =
            api.getCommit(owner, repo, commitSha)

        override suspend fun createBlob(owner: String, repo: String, body: GitHubBlobRequest): GitHubBlobResponse =
            api.createBlob(owner, repo, body)

        override suspend fun createTree(owner: String, repo: String, body: GitHubCreateTreeRequest): GitHubTreeResponse =
            api.createTree(owner, repo, body)

        override suspend fun createCommit(owner: String, repo: String, body: GitHubCreateCommitRequest): GitHubCommitResponse =
            api.createCommit(owner, repo, body)

        override suspend fun updateBranchRef(owner: String, repo: String, branch: String, body: GitHubUpdateRefRequest): GitHubRefResponse =
            api.updateBranchRef(owner, repo, branch, body)
    }
}

private fun HttpException.toReadableMessage(prefix: String): String {
    val details = errorResponseText()
    val parsed = details.parseGitHubErrorDetails()
    return if (parsed.isBlank()) {
        "$prefix: ${code()}"
    } else {
        "$prefix: ${code()} - $parsed"
    }
}

private fun HttpException.errorResponseText(): String = runCatching {
    val body = response()?.errorBody() ?: return ""
    val source = body.source()
    source.request(Long.MAX_VALUE)
    source.buffer.clone().readUtf8()
}.getOrDefault("")

private fun String.parseGitHubErrorDetails(): String {
    if (isBlank()) return ""
    return runCatching {
        val root = githubJson.parseToJsonElement(this).jsonObject
        val message = root["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val nested = root["errors"]?.jsonArray
            ?.mapNotNull { element ->
                val objectValue = element.jsonObject
                objectValue["message"]?.jsonPrimitive?.contentOrNull
                    ?: objectValue["code"]?.jsonPrimitive?.contentOrNull
            }
            .orEmpty()
        listOf(message, nested.joinToString("；"))
            .filter { it.isNotBlank() }
            .joinToString("；")
    }.getOrDefault("")
}

private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
private fun File.readAsBase64(): String = readBytes().toBase64()
