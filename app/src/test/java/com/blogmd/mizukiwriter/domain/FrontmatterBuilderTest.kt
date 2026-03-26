package com.blogmd.mizukiwriter.domain

import com.blogmd.mizukiwriter.data.model.DraftPost
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrontmatterBuilderTest {

    @Test
    fun `writes required and populated optional fields only`() {
        val draft = DraftPost(
            title = "Markdown Tutorial",
            published = "2025-01-20",
            updated = "2025-01-21",
            description = "A simple example",
            tags = listOf("Markdown", "Blogging"),
            category = "Examples",
            pinned = true,
            draft = false,
            comment = false,
            date = "2025-01-20",
            pubDate = "2025-01-20",
            image = "./cover.png",
            author = "emn178",
            sourceLink = "https://github.com/emn178/markdown",
            licenseName = "Unlicensed",
            licenseUrl = "https://example.com/license",
            permalink = "encrypted-example",
            alias = listOf("markdown-tutorial", "markdown-guide"),
            lang = "zh-CN",
            priority = 3,
            encrypted = true,
            password = "secret",
            passwordHint = "hint",
        )

        val result = FrontmatterBuilder.build(draft)

        assertThat(result).contains("title: Markdown Tutorial")
        assertThat(result).contains("published: 2025-01-20")
        assertThat(result).contains("updated: 2025-01-21")
        assertThat(result).contains("description: A simple example")
        assertThat(result).contains("tags: [Markdown, Blogging]")
        assertThat(result).contains("category: Examples")
        assertThat(result).contains("pinned: true")
        assertThat(result).contains("draft: false")
        assertThat(result).contains("comment: false")
        assertThat(result).contains("date: 2025-01-20")
        assertThat(result).contains("pubDate: 2025-01-20")
        assertThat(result).contains("image: \"./cover.png\"")
        assertThat(result).contains("author: emn178")
        assertThat(result).contains("sourceLink: \"https://github.com/emn178/markdown\"")
        assertThat(result).contains("licenseName: \"Unlicensed\"")
        assertThat(result).contains("licenseUrl: \"https://example.com/license\"")
        assertThat(result).contains("permalink: \"encrypted-example\"")
        assertThat(result).contains("alias: [markdown-tutorial, markdown-guide]")
        assertThat(result).contains("lang: zh-CN")
        assertThat(result).contains("priority: 3")
        assertThat(result).contains("encrypted: true")
        assertThat(result).contains("password: \"secret\"")
        assertThat(result).contains("passwordHint: \"hint\"")
        assertThat(result).startsWith("---\n")
        assertThat(result).endsWith("---")
    }

    @Test
    fun `throws when required frontmatter is missing`() {
        val draft = DraftPost(title = "", published = "", description = "")

        val error = runCatching { FrontmatterBuilder.build(draft) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains("title")
    }

    @Test
    fun `throws when description is missing`() {
        val draft = DraftPost(title = "Title", published = "2025-01-20", description = "")

        val error = runCatching { FrontmatterBuilder.build(draft) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().contains("description")
    }
}
