package com.blogmd.mizukiwriter.domain

import com.blogmd.mizukiwriter.data.model.DraftPost
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object RemoteArticleMapper {
    fun fromMarkdown(
        path: String,
        source: String,
    ): DraftPost {
        val document = MarkdownFrontmatterEditor.parse(source)
        val frontmatter = document.frontmatter
        return DraftPost(
            title = frontmatter.stringValue("title"),
            slug = path.toSlug(),
            body = document.body,
            description = frontmatter.stringValue("description"),
            published = frontmatter.stringValue("published"),
            updated = frontmatter.stringValue("updated"),
            date = frontmatter.stringValue("date"),
            pubDate = frontmatter.stringValue("pubDate"),
            tags = frontmatter.stringList("tags"),
            alias = frontmatter.stringList("alias"),
            category = frontmatter.stringValue("category"),
            lang = frontmatter.stringValue("lang"),
            draft = frontmatter.booleanValue("draft"),
            pinned = frontmatter.booleanValue("pinned"),
            comment = frontmatter.booleanValue("comment", default = true),
            priority = frontmatter.intValue("priority"),
            image = frontmatter.stringValue("image"),
            author = frontmatter.stringValue("author"),
            sourceLink = frontmatter.stringValue("sourceLink"),
            licenseName = frontmatter.stringValue("licenseName"),
            licenseUrl = frontmatter.stringValue("licenseUrl"),
            permalink = frontmatter.stringValue("permalink"),
            encrypted = frontmatter.booleanValue("encrypted"),
            password = frontmatter.stringValue("password"),
            passwordHint = frontmatter.stringValue("passwordHint"),
        )
    }

    private fun JsonObject.stringValue(key: String): String =
        (this[key] as? JsonPrimitive)?.content.orEmpty()

    private fun JsonObject.booleanValue(key: String, default: Boolean = false): Boolean {
        val primitive = this[key] as? JsonPrimitive ?: return default
        return primitive.content.toBooleanStrictOrNull() ?: default
    }

    private fun JsonObject.intValue(key: String): Int {
        val primitive = this[key] as? JsonPrimitive ?: return 0
        return primitive.content.toIntOrNull() ?: 0
    }

    private fun JsonObject.stringList(key: String): List<String> =
        (this[key] as? JsonArray)
            ?.mapNotNull { element -> (element as? JsonPrimitive)?.content }
            .orEmpty()

    private fun String.toSlug(): String {
        val normalized = trim('/')
        return if (normalized.endsWith("/index.md")) {
            normalized.substringBeforeLast('/').substringAfterLast('/')
        } else {
            normalized.substringAfterLast('/').substringBeforeLast('.')
        }
    }
}
