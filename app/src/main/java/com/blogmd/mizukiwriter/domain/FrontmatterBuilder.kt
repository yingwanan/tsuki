package com.blogmd.mizukiwriter.domain

import com.blogmd.mizukiwriter.data.model.DraftPost

object FrontmatterBuilder {
    fun build(
        draft: DraftPost,
        defaultAuthor: String = "",
        defaultLicenseName: String = "",
    ): String {
        require(draft.title.isNotBlank()) { "title is required" }
        require(draft.description.isNotBlank()) { "description is required" }
        require(draft.published.isNotBlank()) { "published is required" }

        val resolvedAuthor = draft.author.ifBlank { defaultAuthor }
        val resolvedLicenseName = draft.licenseName.ifBlank { defaultLicenseName }
        val lines = buildList {
            add("---")
            addLine("title", draft.title, quoted = false)
            addLine("published", draft.published, quoted = false)
            addLine("updated", draft.updated, quoted = false)
            addLine("date", draft.date, quoted = false)
            addLine("pubDate", draft.pubDate, quoted = false)
            addLine("pinned", draft.pinned.toString(), quoted = false, include = draft.pinned)
            addLine("description", draft.description, quoted = false)
            if (draft.tags.isNotEmpty()) {
                add("tags: [${draft.tags.joinToString(", ")}]")
            }
            if (draft.alias.isNotEmpty()) {
                add("alias: [${draft.alias.joinToString(", ")}]")
            }
            addLine("category", draft.category, quoted = false)
            addLine("lang", draft.lang, quoted = false)
            addLine("licenseName", resolvedLicenseName, forceQuotes = true)
            addLine("licenseUrl", draft.licenseUrl, forceQuotes = true)
            addLine("author", resolvedAuthor, quoted = false)
            addLine("sourceLink", draft.sourceLink, forceQuotes = true)
            addLine("priority", draft.priority.toString(), quoted = false, include = draft.priority > 0)
            addLine("draft", draft.draft.toString(), quoted = false, include = true)
            addLine("comment", draft.comment.toString(), quoted = false, include = !draft.comment)
            addLine("image", draft.image, forceQuotes = true)
            addLine("permalink", draft.permalink, forceQuotes = true)
            addLine("encrypted", draft.encrypted.toString(), quoted = false, include = draft.encrypted)
            addLine("password", draft.password, forceQuotes = true)
            addLine("passwordHint", draft.passwordHint, forceQuotes = true)
            add("---")
        }

        return lines.joinToString("\n")
    }

    private fun MutableList<String>.addLine(
        key: String,
        value: String,
        quoted: Boolean = true,
        forceQuotes: Boolean = false,
        include: Boolean = value.isNotBlank(),
    ) {
        if (!include) return
        val renderedValue = when {
            forceQuotes -> "\"$value\""
            quoted && needsQuotes(value) -> "\"$value\""
            else -> value
        }
        add("$key: $renderedValue")
    }

    private fun needsQuotes(value: String): Boolean = value.contains("./") || value.contains("://") || value.contains(':')
}
