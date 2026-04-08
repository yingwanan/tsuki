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
            .forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank() || line.startsWith("#")) return@forEach
                val separator = line.indexOfUnquotedColon()
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
        val normalized = rawValue.trim()
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            return JsonArray(parseInlineArray(normalized.removePrefix("[").removeSuffix("]")))
        }
        return when (normalized) {
            "true", "false" -> JsonPrimitive(normalized == "true")
            "null" -> JsonPrimitive("null")
            else -> normalized.toScalarPrimitive()
        }
    }

    private fun renderValue(value: JsonElement): String = when (value) {
        is JsonPrimitive -> renderPrimitive(value)
        is JsonArray -> value.joinToString(prefix = "[", postfix = "]") { element ->
            renderValue(element)
        }
        else -> value.toString()
    }

    private fun parseInlineArray(content: String): List<JsonElement> {
        if (content.isBlank()) return emptyList()
        val items = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaping = false

        for (char in content) {
            if (escaping) {
                current.append(char)
                escaping = false
                continue
            }
            if (char == '\\' && quote != null) {
                current.append(char)
                escaping = true
                continue
            }
            if (quote != null) {
                current.append(char)
                if (char == quote) quote = null
                continue
            }
            when (char) {
                '\'', '"' -> {
                    quote = char
                    current.append(char)
                }
                ',' -> {
                    items += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        items += current.toString().trim()
        return items.filter { it.isNotBlank() }.map { it.toScalarPrimitive() }
    }

    private fun String.indexOfUnquotedColon(): Int {
        var quote: Char? = null
        var escaping = false
        forEachIndexed { index, char ->
            if (escaping) {
                escaping = false
                return@forEachIndexed
            }
            if (char == '\\' && quote != null) {
                escaping = true
                return@forEachIndexed
            }
            if (quote != null) {
                if (char == quote) quote = null
                return@forEachIndexed
            }
            if (char == '"' || char == '\'') {
                quote = char
                return@forEachIndexed
            }
            if (char == ':') return index
        }
        return -1
    }

    private fun String.toScalarPrimitive(): JsonPrimitive {
        val normalized = trim()
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length >= 2) {
            return JsonPrimitive(normalized.substring(1, normalized.length - 1).unescapeQuoted())
        }
        if (normalized.startsWith("'") && normalized.endsWith("'") && normalized.length >= 2) {
            return JsonPrimitive(normalized.substring(1, normalized.length - 1).unescapeQuoted())
        }
        return JsonPrimitive(normalized)
    }

    private fun renderPrimitive(value: JsonPrimitive): String {
        if (!value.isString) return value.content
        val content = value.content
        if (content == "true" || content == "false" || content == "null") return content
        return if (content.requiresQuotes()) {
            "\"${content.escapeForFrontmatter()}\""
        } else {
            content
        }
    }

    private fun String.requiresQuotes(): Boolean {
        if (isBlank()) return true
        return any { char ->
            char == ':' || char == '"' || char == '\'' || char == '[' || char == ']' || char == ','
        }
    }

    private fun String.escapeForFrontmatter(): String = buildString(length) {
        for (char in this@escapeForFrontmatter) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

    private fun String.unescapeQuoted(): String = buildString(length) {
        var escaping = false
        for (char in this@unescapeQuoted) {
            if (escaping) {
                append(
                    when (char) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        else -> char
                    },
                )
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else {
                append(char)
            }
        }
    }
}
