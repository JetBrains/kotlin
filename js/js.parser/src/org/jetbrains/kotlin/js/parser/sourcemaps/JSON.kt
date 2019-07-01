/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.parser.sourcemaps

import java.io.*

sealed class JsonNode {
    abstract fun write(writer: Writer)

    override fun toString(): String = StringWriter().also { write(it) }.toString()
}

object JsonNull : JsonNode() {
    override fun write(writer: Writer) {
        writer.append("null")
    }
}

class JsonBoolean private constructor(val value: Boolean) : JsonNode() {
    private val stringValue = value.toString()

    override fun write(writer: Writer) {
        writer.append(stringValue)
    }

    companion object {
        val TRUE = JsonBoolean(true)
        val FALSE = JsonBoolean(false)

        fun of(value: Boolean): JsonBoolean = if (value) TRUE else FALSE
    }
}

data class JsonObject(val properties: MutableMap<String, JsonNode>) : JsonNode() {
    constructor(vararg properties: Pair<String, JsonNode>) : this(properties.toMap().toMutableMap())

    override fun write(writer: Writer) {
        writer.append('{')
        var first = true
        for ((key, value) in properties) {
            if (!first) {
                writer.append(',')
            }
            first = false
            JsonString(key).write(writer)
            writer.append(':')
            value.write(writer)
        }
        writer.append('}')
    }

    override fun toString(): String = super.toString()
}

data class JsonArray(val elements: MutableList<JsonNode>) : JsonNode() {
    constructor(vararg elements: JsonNode) : this(elements.toMutableList())

    override fun write(writer: Writer) {
        writer.append('[')
        var first = true
        for (element in elements) {
            if (!first) {
                writer.append(',')
            }
            first = false
            element.write(writer)
        }
        writer.append(']')
    }
}

data class JsonString(val value: String) : JsonNode() {
    override fun write(writer: Writer) {
        writer.append('"')
        for (c in value) {
            when (c) {
                '\\' -> writer.append("\\\\")
                '"' -> writer.append("\\\"")
                '\r' -> writer.append("\\r")
                '\n' -> writer.append("\\n")
                '\t' -> writer.append("\\t")
                '\b' -> writer.append("\\b")
                '\u000C' -> writer.append("\\f")
                in ' '..126.toChar() -> writer.append(c)
                else -> {
                    writer.append("\\u")
                    var shift = 16
                    repeat(4) {
                        shift -= 4
                        val digit = (c.toInt() ushr shift) and 0xF
                        writer.append(if (digit < 10) (digit + '0'.toInt()).toChar() else (digit - 10 + 'a'.toInt()).toChar())
                    }
                }
            }
        }
        writer.append('"')
    }

    override fun toString(): String = super.toString()
}

data class JsonNumber(val value: Double) : JsonNode() {
    override fun write(writer: Writer) {
        if (value.toLong().toDouble() == value) {
            writer.append(value.toLong().toString())
        } else {
            writer.append(value.toString())
        }
    }

    override fun toString(): String = super.toString()
}

class JsonSyntaxException(val offset: Int, val line: Int, val column: Int, val text: String) :
    RuntimeException("JSON syntax error at ${line + 1}, ${column + 1}: $text")

fun parseJson(file: File): JsonNode = parseJson(file.readText(Charsets.UTF_8))

fun parseJson(text: String): JsonNode = JsonParser(text).parse()

private class JsonParser(val content: String) {
    private var index = -1
    private var charCode = content.getOrNull(++index)?.toInt() ?: -1
    private var offset = 0
    private var line = 0
    private var col = 0
    private var wasCR = false

    fun parse(): JsonNode {
        val result = parseNode()
        skipSpaces()
        if (charCode != -1) throw error("End of input expected")
        return result
    }

    private fun skipSpaces() {
        while (true) {
            when (charCode) {
                ' '.toInt(), '\t'.toInt(), '\n'.toInt(), '\r'.toInt() -> advance()
                else -> return
            }
        }
    }

    private fun parseNode(): JsonNode {
        skipSpaces()
        return when (charCode) {
            '['.toInt() -> parseArray()
            '{'.toInt() -> parseObject()
            '"'.toInt() -> JsonString(parseString())
            'n'.toInt() -> {
                expectString("null")
                JsonNull
            }
            'f'.toInt() -> {
                expectString("false")
                JsonBoolean.FALSE
            }
            't'.toInt() -> {
                expectString("true")
                JsonBoolean.TRUE
            }
            '-'.toInt() -> {
                advance()
                JsonNumber(-parseNumber())
            }
            else -> if (charCode in '0'.toInt()..'9'.toInt()) {
                JsonNumber(parseNumber())
            } else {
                error("Unexpected char")
            }
        }
    }

    private fun parseArray(): JsonArray {
        advance()
        val result = JsonArray()
        while (true) {
            skipSpaces()
            if (charCode == ']'.toInt()) {
                advance()
                break
            } else {
                if (result.elements.isNotEmpty()) {
                    expectCharAndAdvance(',')
                }
                result.elements += parseNode()
            }
        }
        return result
    }

    private fun parseObject(): JsonObject {
        advance()
        val result = JsonObject()
        while (true) {
            skipSpaces()
            if (charCode == '}'.toInt()) {
                advance()
                break
            } else {
                if (result.properties.isNotEmpty()) {
                    expectCharAndAdvance(',')
                }

                skipSpaces()
                val key = parseString()
                if (key in result.properties) {
                    error("Duplicate property name: $key")
                }

                skipSpaces()
                expectCharAndAdvance(':')

                result.properties[key] = parseNode()
            }
        }
        return result
    }

    private fun parseString(): String {
        expectCharAndAdvance('"')

        val sb = StringBuilder()

        var leftIndex = index
        while (index < content.length) {
            charCode = content[index].toInt()

            if (charCode < ' '.toInt()) {
                error("Invalid character in string literal")
            }

            when (charCode) {
                '"'.toInt() -> {
                    sb.append(content, leftIndex, index)
                    advance()
                    return sb.toString()
                }
                '\\'.toInt() -> {
                    sb.append(content, leftIndex, index)
                    sb.append(parseEscapeSequence())
                    leftIndex = index
                }
                else -> ++index
            }
        }
        error("Unexpected end of file")
    }

    private fun parseEscapeSequence(): Char {
        advance()
        return when (charCode) {
            '"'.toInt() -> advanceAndThen { '"' }
            '\\'.toInt() -> advanceAndThen { '\\' }
            '/'.toInt() -> advanceAndThen { '/' }
            'b'.toInt() -> advanceAndThen { '\b' }
            'n'.toInt() -> advanceAndThen { '\n' }
            'r'.toInt() -> advanceAndThen { '\r' }
            'f'.toInt() -> advanceAndThen { '\u000C' }
            't'.toInt() -> advanceAndThen { '\t' }
            'u'.toInt() -> parseHexEscapeSequence()
            else -> error("Invalid escape sequence")
        }
    }

    private fun parseHexEscapeSequence(): Char {
        advance()
        var value = 0
        repeat(4) {
            value *= 16
            value += when (charCode) {
                in '0'.toInt()..'9'.toInt() -> charCode - '0'.toInt()
                in 'a'.toInt()..'f'.toInt() -> charCode - 'a'.toInt() + 10
                in 'A'.toInt()..'F'.toInt() -> charCode - 'A'.toInt() + 10
                else -> error("Invalid escape sequence, hexadecimal char expected")
            }
            advance()
        }
        return value.toChar()
    }

    private fun parseNumber(): Double {
        val sb = StringBuilder()
        takeIntegerDigitsTo(sb)
        if (sb.startsWith('0') && sb.length > 1) error("Number must not start with zero")

        return when (charCode) {
            '.'.toInt() -> {
                sb.append('.')
                advance()
                takeIntegerDigitsTo(sb)
                if (charCode == 'e'.toInt() || charCode == 'E'.toInt()) {
                    takeExponentTo(sb)
                }
                sb.toString().toDouble()
            }
            'e'.toInt(), 'E'.toInt() -> {
                takeExponentTo(sb)
                sb.toString().toDouble()
            }
            else -> return sb.toString().toDouble()
        }
    }

    private fun takeIntegerDigitsTo(buffer: StringBuilder) {
        var size = 0
        while (charCode in '0'.toInt()..'9'.toInt()) {
            buffer.append(charCode.toChar())
            advance()
            size++
        }
        if (size == 0) error("Invalid char, decimal digit expected")
    }

    private fun takeExponentTo(buffer: StringBuilder) {
        buffer.append('e')
        advance()
        if (charCode == '-'.toInt() || charCode == '+'.toInt()) {
            buffer.append(charCode.toChar())
            advance()
        }
        takeIntegerDigitsTo(buffer)
    }

    private fun expectString(expected: String) {
        for (c in expected) {
            if (charCode != c.toInt()) error("Unexpected char, '$expected' expected here")
            advance()
        }
    }

    private fun expectCharAndAdvance(expected: Char) {
        if (charCode != expected.toInt()) error("Unexpected char, '$expected' expected here")
        advance()
    }

    private inline fun <T> advanceAndThen(f: () -> T): T {
        advance()
        return f()
    }

    private fun advance() {
        if (charCode == -1) return
        when (charCode) {
            '\r'.toInt() -> {
                line++
                col = 0
                wasCR = true
            }
            '\n'.toInt() -> {
                if (!wasCR) {
                    line++
                    col = 0
                    wasCR = false
                }
            }
            else -> {
                col++
                wasCR = false
            }
        }
        charCode = content.getOrNull(++index)?.toInt() ?: -1
        offset++
    }

    private fun error(text: String): Nothing = throw JsonSyntaxException(offset, line, col, text)
}
