package com.blogmd.mizukiwriter.data.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubContentRequest(
    val message: String,
    val content: String,
    val branch: String,
    val sha: String? = null,
)

@Serializable
data class GitHubDeleteRequest(
    val message: String,
    val branch: String,
    val sha: String,
)

@Serializable
data class GitHubContentDocumentResponse(
    val sha: String = "",
    val name: String = "",
    val path: String = "",
    val type: String = "file",
    val content: String = "",
    val encoding: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
)

@Serializable
data class GitHubContentResponse(
    val sha: String = "",
    val name: String = "",
    val path: String = "",
    val type: String = "file",
    val content: GitHubContentDescriptor? = null,
)

@Serializable
data class GitHubContentDescriptor(
    val sha: String = "",
    val path: String = "",
    @SerialName("download_url") val downloadUrl: String? = null,
)

@Serializable
data class GitHubRefResponse(
    @SerialName("ref") val refName: String = "",
    @SerialName("object") val obj: GitHubShaObject = GitHubShaObject(),
)

@Serializable
data class GitHubShaObject(
    val sha: String = "",
)

@Serializable
data class GitHubCommitResponse(
    val sha: String = "",
    val tree: GitHubShaObject = GitHubShaObject(),
)

@Serializable
data class GitHubBlobRequest(
    val content: String,
    val encoding: String = "base64",
)

@Serializable
data class GitHubBlobResponse(
    val sha: String = "",
)

@Serializable
data class GitHubBlobContentResponse(
    val sha: String = "",
    val content: String = "",
    val encoding: String = "base64",
)

@Serializable
data class GitHubTreeEntry(
    val path: String,
    val mode: String = "100644",
    val type: String = "blob",
    val sha: String? = null,
)

@Serializable
data class GitHubCreateTreeRequest(
    @SerialName("base_tree") val baseTree: String,
    val tree: List<GitHubTreeEntry>,
)

@Serializable
data class GitHubTreeResponse(
    val sha: String = "",
    val tree: List<GitHubTreeNode> = emptyList(),
    val truncated: Boolean = false,
)

@Serializable
data class GitHubTreeNode(
    val path: String = "",
    val mode: String = "100644",
    val type: String = "blob",
    val sha: String? = null,
)

@Serializable
data class GitHubCreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>,
)

@Serializable
data class GitHubUpdateRefRequest(
    val sha: String,
    val force: Boolean = false,
)

@Serializable
data class GitHubCreateRefRequest(
    val ref: String,
    val sha: String,
)

@Serializable
data class GitHubRepositoryResponse(
    val name: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("default_branch") val defaultBranch: String = "master",
    val private: Boolean = false,
)

@Serializable
data class GitHubWorkflowRunsResponse(
    @SerialName("workflow_runs") val workflowRuns: List<GitHubWorkflowRun> = emptyList(),
)

@Serializable
data class GitHubWorkflowRun(
    val id: Long = 0L,
    val name: String = "",
    val status: String = "",
    val conclusion: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("head_branch") val headBranch: String = "",
    @SerialName("head_sha") val headSha: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)
