package com.blogmd.mizukiwriter.data.settings

enum class DeploymentPlatform(
    val label: String,
) {
    Vercel("Vercel"),
    CloudflarePages("Cloudflare Pages"),
    EdgeOnePages("EdgeOne Pages"),
}

enum class EdgeOneExecutionMode(
    val label: String,
) {
    GitHubActions("GitHub Actions"),
    LimitedLocalCli("受限本地 CLI"),
}
