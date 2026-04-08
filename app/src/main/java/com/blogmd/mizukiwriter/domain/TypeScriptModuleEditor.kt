package com.blogmd.mizukiwriter.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class TypeScriptExportDocument(
    val bindingName: String,
    val value: JsonElement,
)

object TypeScriptModuleEditor {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun parseExport(source: String, exportName: String): TypeScriptExportDocument {
        val binding = findBinding(source, exportName)
        return TypeScriptExportDocument(bindingName = exportName, value = parseLiteral(binding.literal))
    }

    fun updateExport(
        source: String,
        exportName: String,
        transform: (JsonElement) -> JsonElement,
    ): String {
        val binding = findBinding(source, exportName)
        val current = parseLiteral(binding.literal)
        val updated = renderLiteral(transform(current))
        return source.replaceRange(binding.literalStart, binding.literalEndExclusive, updated)
    }

    fun parseBinding(source: String, bindingName: String): TypeScriptExportDocument {
        val binding = findBinding(source, bindingName)
        return TypeScriptExportDocument(bindingName = bindingName, value = parseLiteral(binding.literal))
    }

    fun updateBinding(
        source: String,
        bindingName: String,
        transform: (JsonElement) -> JsonElement,
    ): String {
        val binding = findBinding(source, bindingName)
        val current = parseLiteral(binding.literal)
        val updated = renderLiteral(transform(current))
        return source.replaceRange(binding.literalStart, binding.literalEndExclusive, updated)
    }

    private fun parseLiteral(literal: String): JsonElement {
        val normalized = literal
            .stripComments()
            .replaceSingleQuotedStrings()
            .quoteObjectKeys()
            .wrapTypeScriptExpressions()
            .removeTrailingCommas()
        return json.parseToJsonElement(normalized)
    }

    private fun renderLiteral(element: JsonElement, indentLevel: Int = 0): String = when (element) {
        is JsonObject -> renderObject(element, indentLevel)
        is JsonArray -> renderArray(element, indentLevel)
        is JsonPrimitive -> renderPrimitive(element)
        JsonNull -> "null"
    }

    private fun renderObject(element: JsonObject, indentLevel: Int): String {
        if (element.isEmpty()) return "{}"
        val indent = "\t".repeat(indentLevel)
        val childIndent = "\t".repeat(indentLevel + 1)
        val body = element.entries.joinToString(",\n") { (key, value) ->
            "$childIndent${renderKey(key)}: ${renderLiteral(value, indentLevel + 1)}"
        }
        return "{\n$body\n$indent}"
    }

    private fun renderArray(element: JsonArray, indentLevel: Int): String {
        if (element.isEmpty()) return "[]"
        val inline = element.all { child ->
            child is JsonPrimitive || child is JsonNull
        }
        return if (inline) {
            element.joinToString(prefix = "[", postfix = "]") { renderLiteral(it, indentLevel) }
        } else {
            val indent = "\t".repeat(indentLevel)
            val childIndent = "\t".repeat(indentLevel + 1)
            val body = element.joinToString(",\n") { child ->
                "$childIndent${renderLiteral(child, indentLevel + 1)}"
            }
            "[\n$body\n$indent]"
        }
    }

    private fun renderPrimitive(element: JsonPrimitive): String {
        if (element.isString) {
            if (element.content.startsWith(TS_EXPRESSION_PREFIX)) {
                return element.content.removePrefix(TS_EXPRESSION_PREFIX)
            }
            return "\"${element.content.escapeString()}\""
        }
        return element.content
    }

    private fun renderKey(key: String): String {
        return if (SAFE_IDENTIFIER.matches(key)) key else "\"${key.escapeString()}\""
    }

    private fun findBinding(source: String, bindingName: String): BindingSlice {
        val constRegex = Regex("""(?:export\s+)?const\s+$bindingName\b""")
        val match = constRegex.find(source)
            ?: error("未找到绑定：$bindingName")
        val equalsIndex = source.indexOf('=', match.range.last + 1)
        require(equalsIndex >= 0) { "绑定缺少赋值：$bindingName" }
        val literalStart = source.indexOfFirstNonWhitespace(equalsIndex + 1)
        require(literalStart >= 0) { "绑定值为空：$bindingName" }
        val opener = source[literalStart]
        require(opener == '{' || opener == '[') { "仅支持对象或数组字面量：$bindingName" }
        val literalEndExclusive = source.findLiteralEndExclusive(literalStart)
        return BindingSlice(
            literalStart = literalStart,
            literalEndExclusive = literalEndExclusive,
            literal = source.substring(literalStart, literalEndExclusive),
        )
    }

    private fun String.indexOfFirstNonWhitespace(startIndex: Int): Int {
        for (index in startIndex until length) {
            if (!this[index].isWhitespace()) return index
        }
        return -1
    }

    private fun String.findLiteralEndExclusive(startIndex: Int): Int {
        var depth = 0
        var inString = false
        var stringQuote = '\u0000'
        var escaping = false
        var inLineComment = false
        var inBlockComment = false

        for (index in startIndex until length) {
            val current = this[index]
            val next = getOrNull(index + 1)

            if (inLineComment) {
                if (current == '\n') inLineComment = false
                continue
            }
            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false
                }
                continue
            }
            if (inString) {
                if (escaping) {
                    escaping = false
                    continue
                }
                if (current == '\\') {
                    escaping = true
                    continue
                }
                if (current == stringQuote) {
                    inString = false
                }
                continue
            }
            if (current == '/' && next == '/') {
                inLineComment = true
                continue
            }
            if (current == '/' && next == '*') {
                inBlockComment = true
                continue
            }
            if (current == '"' || current == '\'') {
                inString = true
                stringQuote = current
                continue
            }
            if (current == '{' || current == '[') depth++
            if (current == '}' || current == ']') {
                depth--
                if (depth == 0) return index + 1
            }
        }
        error("未找到字面量结束位置")
    }

    private fun String.stripComments(): String {
        val result = StringBuilder(length)
        var index = 0
        var inString = false
        var stringQuote = '\u0000'
        var escaping = false

        while (index < length) {
            val current = this[index]
            val next = getOrNull(index + 1)

            if (inString) {
                result.append(current)
                if (escaping) {
                    escaping = false
                } else if (current == '\\') {
                    escaping = true
                } else if (current == stringQuote) {
                    inString = false
                }
                index++
                continue
            }

            if (current == '"' || current == '\'') {
                inString = true
                stringQuote = current
                result.append(current)
                index++
                continue
            }

            if (current == '/' && next == '/') {
                index += 2
                while (index < length && this[index] != '\n') index++
                continue
            }
            if (current == '/' && next == '*') {
                index += 2
                while (index + 1 < length && !(this[index] == '*' && this[index + 1] == '/')) index++
                index += 2
                continue
            }

            result.append(current)
            index++
        }

        return result.toString()
    }

    private fun String.replaceSingleQuotedStrings(): String {
        val result = StringBuilder(length)
        var index = 0
        while (index < length) {
            val current = this[index]
            if (current != '\'') {
                result.append(current)
                index++
                continue
            }

            index++
            val content = StringBuilder()
            var escaping = false
            while (index < length) {
                val char = this[index]
                if (escaping) {
                    content.append(char)
                    escaping = false
                    index++
                    continue
                }
                if (char == '\\') {
                    content.append(char)
                    escaping = true
                    index++
                    continue
                }
                if (char == '\'') {
                    break
                }
                content.append(char)
                index++
            }
            result.append('"')
            result.append(content.toString().replace("\"", "\\\""))
            result.append('"')
            index++
        }
        return result.toString()
    }

    private fun String.quoteObjectKeys(): String {
        val regex = Regex("""([{\[,]\s*)([A-Za-z_\u0080-\uFFFF][A-Za-z0-9_\u0080-\uFFFF-]*)\s*:""")
        var current = this
        while (true) {
            val updated = regex.replace(current) { match ->
                "${match.groupValues[1]}\"${match.groupValues[2]}\":"
            }
            if (updated == current) return updated
            current = updated
        }
    }

    private fun String.wrapTypeScriptExpressions(): String {
        val objectValueRegex = Regex("""(:\s*)([A-Za-z_\u0080-\uFFFF][A-Za-z0-9_\u0080-\uFFFF.]*)((?:\s*[,}\]]))""")
        val arrayValueRegex = Regex("""([\[,]\s*)([A-Za-z_\u0080-\uFFFF][A-Za-z0-9_\u0080-\uFFFF.]*)((?:\s*[,}\]]))""")
        return arrayValueRegex.replace(
            objectValueRegex.replace(this) { match ->
                wrapExpressionMatch(match.groupValues[1], match.groupValues[2], match.groupValues[3])
            },
        ) { match ->
            wrapExpressionMatch(match.groupValues[1], match.groupValues[2], match.groupValues[3])
        }
    }

    private fun wrapExpressionMatch(prefix: String, token: String, suffix: String): String {
        if (token == "true" || token == "false" || token == "null") {
            return "$prefix$token$suffix"
        }
        return "$prefix\"$TS_EXPRESSION_PREFIX$token\"$suffix"
    }

    private fun String.removeTrailingCommas(): String =
        replace(Regex(""",(\s*[}\]])"""), "$1")

    private fun String.escapeString(): String = buildString(length) {
        for (char in this@escapeString) {
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

    private data class BindingSlice(
        val literalStart: Int,
        val literalEndExclusive: Int,
        val literal: String,
    )

    private val SAFE_IDENTIFIER = Regex("""[A-Za-z_\u0080-\uFFFF][A-Za-z0-9_\u0080-\uFFFF]*""")
    private const val TS_EXPRESSION_PREFIX = "__ts_expr__:"
}
