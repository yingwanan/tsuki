package com.blogmd.mizukiwriter.data.settings

data class GitHubSettings(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "master",
    val postsBasePath: String = "src/content/posts",
    val personalAccessToken: String = "",
    val defaultAuthor: String = "",
    val defaultLicenseName: String = "",
)
