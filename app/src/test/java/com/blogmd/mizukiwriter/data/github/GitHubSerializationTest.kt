package com.blogmd.mizukiwriter.data.github

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GitHubSerializationTest {

    @Test
    fun `serializes git tree entry defaults required by GitHub`() {
        val payload = githubJson.encodeToString(
            GitHubCreateTreeRequest.serializer(),
            GitHubCreateTreeRequest(
                baseTree = "tree-base",
                tree = listOf(
                    GitHubTreeEntry(
                        path = "src/content/posts/demo/index.md",
                        sha = "blob-1",
                    ),
                ),
            ),
        )

        assertThat(payload).contains("\"mode\":\"100644\"")
        assertThat(payload).contains("\"type\":\"blob\"")
    }

    @Test
    fun `serializes blob encoding as base64`() {
        val payload = githubJson.encodeToString(
            GitHubBlobRequest.serializer(),
            GitHubBlobRequest(content = "Ym9keQ=="),
        )

        assertThat(payload).contains("\"encoding\":\"base64\"")
    }
}
