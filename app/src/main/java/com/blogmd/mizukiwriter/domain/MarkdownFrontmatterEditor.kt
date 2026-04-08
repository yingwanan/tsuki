package com.blogmd.mizukiwriter.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class MarkdownDocument(
    val frontmatter: JsonObject,
    val body: String,
)

object MarkdownFrontmatterEditor {
    fun parse(source: String): MarkdownDocument {
        val normalized = source.replace("\r\n", "\n")
        if (!normalized.startsWith("---\n")) {
            return MarkdownDocument(frontmatter = JsonObject(emptyMap()), body = normalized)
        }

        val end = normalized.indexOf("\n---\n", startIndex = 4)
        if (end < 0) {
            return MarkdownDocument(frontmatter = JsonObject(emptyMap()), body = normalized)
        }

        val frontmatterBlock = normalized.substring(4, end)
        val body = normalized.substring(end + 5).trimStart('\n')
        val values = linkedMapOf<String, JsonElement>()
        frontmatterBlock.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val separator = line.indexOf(':')
                if (separator <= 0) return@forEach
                val key = line.substring(0, separator).trim()
                val rawValue = line.substring(separator + 1).trim()
                values[key] = parseValue(rawValue)
            }

        return MarkdownDocument(frontmatter = JsonObject(values), body = body)
    }

    fun update(frontmatter: JsonObject, body: String): String {
        if (frontmatter.isEmpty()) return body.trimEnd() + "\n"
        val frontmatterText = frontmatter.entries.joinToString("\n") { (key, value) ->
            "$key: ${renderValue(value)}"
        }
        return buildString {
            append("---\n")
            append(frontmatterText)
            append("\n---\n\n")
            append(body.trimEnd())
            append("\n")
        }
    }

    private fun parseValue(rawValue: String): JsonElement {
        if (rawValue.startsWith("[") && rawValue.endsWith("]")) {
            val content = rawValue.removePrefix("[").removeSuffix("]")
            val items = content.split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { item -> JsonPrimitive(item.removeSurrounding("\"")) }
            return JsonArray(items)
        }
        return when (rawValue) {
            "true", "false" -> JsonPrimitive(rawValue == "true")
            else -> JsonPrimitive(rawValue.removeSurrounding("\""))
        }
    }

    private fun renderValue(value: JsonElement): String = when (value) {
        is JsonPrimitive -> value.content
        is JsonArray -> value.joinToString(prefix = "[", postfix = "]") { element ->
            (element as? JsonPrimitive)?.content.orEmpty()
        }
        else -> value.toString()
    }
}
