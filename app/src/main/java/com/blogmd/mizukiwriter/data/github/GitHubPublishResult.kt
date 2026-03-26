package com.blogmd.mizukiwriter.data.github

sealed interface GitHubPublishResult {
    data class Success(val slug: String) : GitHubPublishResult
    data class Conflict(val slug: String, val path: String) : GitHubPublishResult
    data class Failure(val message: String) : GitHubPublishResult
}

sealed interface GitHubDeleteResult {
    data class Success(val slug: String) : GitHubDeleteResult
    data class Failure(val message: String) : GitHubDeleteResult
}
