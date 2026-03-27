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
data class GitHubContentResponse(
    val sha: String = "",
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
