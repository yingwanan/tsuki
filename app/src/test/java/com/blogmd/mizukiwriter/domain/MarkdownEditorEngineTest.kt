package com.blogmd.mizukiwriter.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MarkdownEditorEngineTest {

    @Test
    fun `wraps selection for bold action`() {
        val result = MarkdownEditorEngine.apply(
            text = "hello world",
            selectionStart = 6,
            selectionEnd = 11,
            action = MarkdownAction.Bold,
        )

        assertThat(result.text).isEqualTo("hello **world**")
        assertThat(result.selectionStart).isEqualTo(8)
        assertThat(result.selectionEnd).isEqualTo(13)
    }

    @Test
    fun `inserts code block template when no selection`() {
        val result = MarkdownEditorEngine.apply(
            text = "",
            selectionStart = 0,
            selectionEnd = 0,
            action = MarkdownAction.CodeBlock(language = "kotlin"),
        )

        assertThat(result.text).isEqualTo("```kotlin\n\n```")
        assertThat(result.selectionStart).isEqualTo(10)
        assertThat(result.selectionEnd).isEqualTo(10)
    }

    @Test
    fun `inserts image markdown using relative path`() {
        val result = MarkdownEditorEngine.apply(
            text = "Body",
            selectionStart = 4,
            selectionEnd = 4,
            action = MarkdownAction.Image(relativePath = "./cover.png", alt = "cover"),
        )

        assertThat(result.text).isEqualTo("Body\n\n![cover](./cover.png)")
    }

    @Test
    fun `inserts task list template`() {
        val result = MarkdownEditorEngine.apply(
            text = "",
            selectionStart = 0,
            selectionEnd = 0,
            action = MarkdownAction.TaskList,
        )

        assertThat(result.text).isEqualTo("- [ ] task")
    }

    @Test
    fun `wraps selection in centered html block`() {
        val result = MarkdownEditorEngine.apply(
            text = "center me",
            selectionStart = 0,
            selectionEnd = 9,
            action = MarkdownAction.CenterBlock,
        )

        assertThat(result.text).isEqualTo("<div align=\"center\">\ncenter me\n</div>")
    }
}
