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

    private fun parseLiteral(literal: String): JsonElement = LiteralParser(literal).parse()

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

    private class LiteralParser(
        private val source: String,
    ) {
        private var index: Int = 0

        fun parse(): JsonElement {
            skipIgnorable()
            val value = parseValue()
            skipIgnorable()
            return value
        }

        private fun parseValue(): JsonElement {
            skipIgnorable()
            return when (val current = currentChar()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"', '\'', '`' -> JsonPrimitive(parseString(current))
                null -> error("配置值为空")
                else -> {
                    if (current == '-' || current.isDigit()) {
                        parseNumberOrExpression()
                    } else {
                        parseKeywordOrExpression()
                    }
                }
            }
        }

        private fun parseObject(): JsonObject {
            expect('{')
            val entries = linkedMapOf<String, JsonElement>()
            skipIgnorable()
            if (currentChar() == '}') {
                index++
                return JsonObject(entries)
            }
            while (index < source.length) {
                skipIgnorable()
                val key = parseObjectKey()
                skipIgnorable()
                expect(':')
                val value = parseValue()
                entries[key] = value
                skipIgnorable()
                when (currentChar()) {
                    ',' -> {
                        index++
                        skipIgnorable()
                        if (currentChar() == '}') {
                            index++
                            return JsonObject(entries)
                        }
                    }
                    '}' -> {
                        index++
                        return JsonObject(entries)
                    }
                    else -> error("对象语法无效，字段 `$key` 后缺少逗号或右花括号")
                }
            }
            error("对象未正常结束")
        }

        private fun parseArray(): JsonArray {
            expect('[')
            val items = mutableListOf<JsonElement>()
            skipIgnorable()
            if (currentChar() == ']') {
                index++
                return JsonArray(items)
            }
            while (index < source.length) {
                items += parseValue()
                skipIgnorable()
                when (currentChar()) {
                    ',' -> {
                        index++
                        skipIgnorable()
                        if (currentChar() == ']') {
                            index++
                            return JsonArray(items)
                        }
                    }
                    ']' -> {
                        index++
                        return JsonArray(items)
                    }
                    else -> error("数组语法无效，元素后缺少逗号或右中括号")
                }
            }
            error("数组未正常结束")
        }

        private fun parseObjectKey(): String {
            skipIgnorable()
            return when (val current = currentChar()) {
                '"', '\'' -> parseString(current)
                else -> parseIdentifier()
            }
        }

        private fun parseString(quote: Char): String {
            expect(quote)
            val result = StringBuilder()
            var escaping = false
            while (index < source.length) {
                val char = source[index++]
                if (escaping) {
                    result.append(
                        when (char) {
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            else -> char
                        },
                    )
                    escaping = false
                    continue
                }
                if (char == '\\') {
                    escaping = true
                    continue
                }
                if (char == quote) {
                    return result.toString()
                }
                result.append(char)
            }
            error("字符串未正常结束")
        }

        private fun parseNumberOrExpression(): JsonElement {
            val start = index
            if (currentChar() == '-') index++
            while (currentChar()?.isDigit() == true) index++
            if (currentChar() == '.') {
                index++
                while (currentChar()?.isDigit() == true) index++
            }
            val token = source.substring(start, index)
            return token.toDoubleOrNull()?.let {
                if (token.contains('.')) JsonPrimitive(it) else JsonPrimitive(token.toLong())
            } ?: JsonPrimitive(TS_EXPRESSION_PREFIX + readRawExpression(start))
        }

        private fun parseKeywordOrExpression(): JsonElement {
            val start = index
            val expression = readRawExpression(start)
            return when (expression) {
                "true" -> JsonPrimitive(true)
                "false" -> JsonPrimitive(false)
                "null" -> JsonNull
                else -> JsonPrimitive(TS_EXPRESSION_PREFIX + expression)
            }
        }

        private fun readRawExpression(start: Int): String {
            index = start
            var parenDepth = 0
            var braceDepth = 0
            var bracketDepth = 0
            var quote: Char? = null
            var escaping = false

            while (index < source.length) {
                val char = source[index]
                if (quote != null) {
                    index++
                    if (escaping) {
                        escaping = false
                    } else if (char == '\\') {
                        escaping = true
                    } else if (char == quote) {
                        quote = null
                    }
                    continue
                }
                when (char) {
                    '"', '\'', '`' -> {
                        quote = char
                        index++
                    }
                    '(' -> {
                        parenDepth++
                        index++
                    }
                    ')' -> {
                        if (parenDepth == 0) break
                        parenDepth--
                        index++
                    }
                    '{' -> {
                        braceDepth++
                        index++
                    }
                    '}' -> {
                        if (braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) break
                        braceDepth--
                        index++
                    }
                    '[' -> {
                        bracketDepth++
                        index++
                    }
                    ']' -> {
                        if (bracketDepth == 0 && parenDepth == 0 && braceDepth == 0) break
                        bracketDepth--
                        index++
                    }
                    ',' -> {
                        if (parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) break
                        index++
                    }
                    else -> index++
                }
            }
            return source.substring(start, index).trim()
        }

        private fun parseIdentifier(): String {
            val start = index
            require(currentChar()?.isIdentifierStart() == true) { "字段名必须是标识符或字符串" }
            index++
            while (currentChar()?.isIdentifierPart() == true || currentChar() == '-') {
                index++
            }
            return source.substring(start, index)
        }

        private fun skipIgnorable() {
            while (index < source.length) {
                when (source[index]) {
                    ' ', '\n', '\r', '\t' -> index++
                    '/' -> {
                        val next = source.getOrNull(index + 1)
                        when (next) {
                            '/' -> {
                                index += 2
                                while (index < source.length && source[index] != '\n') index++
                            }
                            '*' -> {
                                index += 2
                                while (index + 1 < source.length && !(source[index] == '*' && source[index + 1] == '/')) {
                                    index++
                                }
                                index += 2
                            }
                            else -> return
                        }
                    }
                    else -> return
                }
            }
        }

        private fun currentChar(): Char? = source.getOrNull(index)

        private fun expect(char: Char) {
            require(currentChar() == char) { "期望字符 `$char`，实际是 `${currentChar()}`" }
            index++
        }

        private fun Char.isIdentifierStart(): Boolean =
            this == '_' || isLetter()

        private fun Char.isIdentifierPart(): Boolean =
            this == '_' || this == '$' || isLetterOrDigit() || this == '.'
    }
}
