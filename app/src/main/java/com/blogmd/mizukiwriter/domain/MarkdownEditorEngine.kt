package com.blogmd.mizukiwriter.domain

data class MarkdownEditResult(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
)

sealed interface MarkdownAction {
    data object Bold : MarkdownAction
    data object Italic : MarkdownAction
    data object Strikethrough : MarkdownAction
    data object Quote : MarkdownAction
    data object BulletList : MarkdownAction
    data object OrderedList : MarkdownAction
    data object TaskList : MarkdownAction
    data object Divider : MarkdownAction
    data object InlineCode : MarkdownAction
    data object CenterBlock : MarkdownAction
    data class CodeBlock(val language: String = "") : MarkdownAction
    data class Image(val relativePath: String, val alt: String = "image") : MarkdownAction
    data class Link(val label: String, val url: String) : MarkdownAction
    data class Heading(val level: Int) : MarkdownAction
}

object MarkdownEditorEngine {
    fun apply(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        action: MarkdownAction,
    ): MarkdownEditResult {
        val safeStart = selectionStart.coerceAtLeast(0).coerceAtMost(text.length)
        val safeEnd = selectionEnd.coerceAtLeast(safeStart).coerceAtMost(text.length)
        val selected = text.substring(safeStart, safeEnd)

        return when (action) {
            MarkdownAction.Bold -> wrapSelection(text, safeStart, safeEnd, selected, "**", "**")
            MarkdownAction.Italic -> wrapSelection(text, safeStart, safeEnd, selected, "*", "*")
            MarkdownAction.Strikethrough -> wrapSelection(text, safeStart, safeEnd, selected, "~~", "~~")
            MarkdownAction.Quote -> insertTemplate(text, safeStart, safeEnd, "> ${selected.ifBlank { "quote" }}")
            MarkdownAction.BulletList -> insertTemplate(text, safeStart, safeEnd, "- ${selected.ifBlank { "item" }}")
            MarkdownAction.OrderedList -> insertTemplate(text, safeStart, safeEnd, "1. ${selected.ifBlank { "item" }}")
            MarkdownAction.TaskList -> insertTemplate(text, safeStart, safeEnd, "- [ ] ${selected.ifBlank { "task" }}")
            MarkdownAction.Divider -> insertTemplate(text, safeStart, safeEnd, "\n\n---\n\n")
            MarkdownAction.InlineCode -> wrapSelection(text, safeStart, safeEnd, selected, "`", "`")
            MarkdownAction.CenterBlock -> insertCenteredBlock(text, safeStart, safeEnd, selected)
            is MarkdownAction.CodeBlock -> insertCodeBlock(text, safeStart, safeEnd, action.language)
            is MarkdownAction.Image -> insertImage(text, safeStart, safeEnd, action.relativePath, action.alt)
            is MarkdownAction.Link -> insertTemplate(text, safeStart, safeEnd, "[${action.label}](${action.url})")
            is MarkdownAction.Heading -> insertHeading(text, safeStart, safeEnd, action.level, selected)
        }
    }

    private fun wrapSelection(
        text: String,
        start: Int,
        end: Int,
        selection: String,
        prefix: String,
        suffix: String,
    ): MarkdownEditResult {
        val replacement = prefix + selection.ifBlank { "text" } + suffix
        val newText = text.replaceRange(start, end, replacement)
        val cursorStart = start + prefix.length
        val cursorEnd = cursorStart + selection.ifBlank { "text" }.length
        return MarkdownEditResult(newText, cursorStart, cursorEnd)
    }

    private fun insertTemplate(
        text: String,
        start: Int,
        end: Int,
        template: String,
    ): MarkdownEditResult {
        val newText = text.replaceRange(start, end, template)
        val cursor = start + template.length
        return MarkdownEditResult(newText, cursor, cursor)
    }

    private fun insertCodeBlock(
        text: String,
        start: Int,
        end: Int,
        language: String,
    ): MarkdownEditResult {
        val languageSuffix = language.trim()
        val template = buildString {
            append("```")
            append(languageSuffix)
            append("\n\n```")
        }
        val newText = text.replaceRange(start, end, template)
        val cursor = start + 3 + languageSuffix.length + 1
        return MarkdownEditResult(newText, cursor, cursor)
    }

    private fun insertImage(
        text: String,
        start: Int,
        end: Int,
        relativePath: String,
        alt: String,
    ): MarkdownEditResult {
        val prefix = if (start > 0 && text[start - 1] != '\n') "\n\n" else ""
        val template = "$prefix![${alt.ifBlank { "image" }}]($relativePath)"
        return insertTemplate(text, start, end, template)
    }

    private fun insertHeading(
        text: String,
        start: Int,
        end: Int,
        level: Int,
        selection: String,
    ): MarkdownEditResult {
        val prefix = "#".repeat(level.coerceIn(1, 6))
        return insertTemplate(text, start, end, "$prefix ${selection.ifBlank { "Heading" }}")
    }

    private fun insertCenteredBlock(
        text: String,
        start: Int,
        end: Int,
        selection: String,
    ): MarkdownEditResult {
        val content = selection.ifBlank { "centered text" }
        val template = "<div align=\"center\">\n$content\n</div>"
        return insertTemplate(text, start, end, template)
    }
}
