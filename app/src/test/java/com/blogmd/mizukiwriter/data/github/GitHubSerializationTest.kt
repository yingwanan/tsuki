package com.blogmd.mizukiwriter.data.github

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GitHubSerializationTest {

    @Test
    fun `deserializes GitHub contents file response with inline base64 content`() {
        val response = githubJson.decodeFromString(
            GitHubContentDocumentResponse.serializer(),
            """
            {
              "name": "config.ts",
              "path": "src/config.ts",
              "sha": "file-sha",
              "type": "file",
              "content": "ZXhwb3J0IGNvbnN0IHNpdGVDb25maWcgPSB7fTs=",
              "encoding": "base64",
              "download_url": "https://raw.githubusercontent.com/demo/blog/master/src/config.ts"
            }
            """.trimIndent(),
        )

        assertThat(response.path).isEqualTo("src/config.ts")
        assertThat(response.content).contains("ZXhwb3J0")
        assertThat(response.encoding).isEqualTo("base64")
    }

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
