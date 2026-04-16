package com.blogmd.mizukiwriter.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RemoteArticleMapperTest {

    @Test
    fun `maps remote markdown article into draft post`() {
        val draft = RemoteArticleMapper.fromMarkdown(
            path = "src/content/posts/hello-world/index.md",
            source = """
                ---
                title: 远程标题
                published: 2025-01-20
                updated: 2025-01-21
                description: 远程简介
                tags: [Android, Compose]
                category: 技术
                author: tester
                ---

                正文内容
            """.trimIndent(),
        )

        assertThat(draft.title).isEqualTo("远程标题")
        assertThat(draft.slug).isEqualTo("hello-world")
        assertThat(draft.description).isEqualTo("远程简介")
        assertThat(draft.tags).containsExactly("Android", "Compose")
        assertThat(draft.category).isEqualTo("技术")
        assertThat(draft.author).isEqualTo("tester")
        assertThat(draft.body.trim()).isEqualTo("正文内容")
    }
}
