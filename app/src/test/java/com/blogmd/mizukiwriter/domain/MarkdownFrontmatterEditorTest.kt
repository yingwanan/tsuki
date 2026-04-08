package com.blogmd.mizukiwriter.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class MarkdownFrontmatterEditorTest {

    @Test
    fun `parses frontmatter and markdown body`() {
        val document = MarkdownFrontmatterEditor.parse(
            """
            ---
            title: Draft Example
            draft: true
            tags: [Markdown, Blogging, Demo]
            ---

            # Hello
            """.trimIndent(),
        )

        val frontmatter = document.frontmatter
        assertThat((frontmatter["title"] as JsonPrimitive).content).isEqualTo("Draft Example")
        assertThat((frontmatter["draft"] as JsonPrimitive).content).isEqualTo("true")
        assertThat((frontmatter["tags"] as JsonArray).size).isEqualTo(3)
        assertThat(document.body.trim()).isEqualTo("# Hello")
    }

    @Test
    fun `updates frontmatter fields and preserves markdown body`() {
        val updated = MarkdownFrontmatterEditor.update(
            frontmatter = JsonObject(
                linkedMapOf(
                    "title" to JsonPrimitive("Updated Title"),
                    "draft" to JsonPrimitive(false),
                    "tags" to JsonArray(listOf(JsonPrimitive("Diary"), JsonPrimitive("Remote"))),
                ),
            ),
            body = "## Body",
        )

        assertThat(updated).contains("title: Updated Title")
        assertThat(updated).contains("draft: false")
        assertThat(updated).contains("tags: [Diary, Remote]")
        assertThat(updated).contains("## Body")
    }

    @Test
    fun `parses quoted description with colon and apostrophe`() {
        val document = MarkdownFrontmatterEditor.parse(
            """
            ---
            title: "中文标题"
            description: "Girl's gunfight: React Hooks, Context, and state"
            tags: ["A", "B"]
            ---

            正文
            """.trimIndent(),
        )

        val frontmatter = document.frontmatter
        assertThat((frontmatter["title"] as JsonPrimitive).content).isEqualTo("中文标题")
        assertThat((frontmatter["description"] as JsonPrimitive).content)
            .isEqualTo("Girl's gunfight: React Hooks, Context, and state")
        assertThat((frontmatter["tags"] as JsonArray).size).isEqualTo(2)
    }
}
