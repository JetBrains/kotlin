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

@file:Suppress("DEPRECATION") // TODO: needs an intensive rework for new Char API
package org.jetbrains.kotlin.js.parser.sourcemaps

import java.io.File
import java.io.IOException
import java.io.StringReader

object SourceMapParser {
    @Throws(IOException::class)
    fun parse(file: File): SourceMapParseResult {
        return parse(file.readText(Charsets.UTF_8))
    }

    @Throws(IOException::class)
    fun parse(content: String): SourceMapParseResult {
        val jsonObject = try {
            parseJson(content)
        }
        catch (e: JsonSyntaxException) {
            return SourceMapError(e.message ?: "parse error")
        }
        return parse(jsonObject)
    }

    @Throws(IOException::class)
    private fun parse(jsonObject: JsonNode): SourceMapParseResult {
        if (jsonObject !is JsonObject) return SourceMapError("Top-level object expected")

        val version = jsonObject.properties["version"] ?: return SourceMapError("Version not defined")
        version.let {
            if (version !is JsonNumber || version.value != 3.0) return SourceMapError("Unsupported version: $it")
        }

        val sourceRoot = jsonObject.properties["sourceRoot"].let {
            if (it != null) {
                (it as? JsonString ?: return SourceMapError("'sourceRoot' property is not of string type")).value
            }
            else {
                ""
            }
        }

        val sources = jsonObject.properties["sources"].let {
            if (it != null) {
                val sourcesProperty = it as? JsonArray ?:
                                      return SourceMapError("'sources' property is not of array type")
                sourcesProperty.elements.map {
                    (it as? JsonString ?: return SourceMapError("'sources' array must contain strings")).value
                }
            }
            else {
                emptyList()
            }
        }

        val sourcesContent: List<String?> = jsonObject.properties["sourcesContent"].let {
            if (it != null) {
                val sourcesContentProperty = it as? JsonArray ?:
                                             return SourceMapError("'sourcesContent' property is not of array type")
                sourcesContentProperty.elements.map {
                    when (it) {
                        is JsonNull -> null
                        is JsonString -> it.value
                        else -> return SourceMapError("'sources' array must contain strings")
                    }
                }
            }
            else {
                emptyList()
            }
        }

        val names = jsonObject.properties["names"].let {
            if (it != null) {
                val namesProperty = it as? JsonArray ?: return SourceMapError("'names' property is not of array type")
                namesProperty.elements.map {
                    (it as? JsonString ?: return SourceMapError("'names' array must contain strings")).value
                }
            } else {
                emptyList()
            }
        }

        val sourcePathToContent = sources.zip(sourcesContent).associate { it }

        val mappings = jsonObject.properties["mappings"] ?: return SourceMapError("'mappings' property not found")
        if (mappings !is JsonString) return SourceMapError("'mappings' property is not of string type")

        var jsColumn = 0
        var sourceLine = 0
        var sourceColumn = 0
        var sourceIndex = 0
        var nameIndex = 0
        val stream = MappingStream(mappings.value)
        val sourceMap = SourceMap { sourcePathToContent[it]?.let { StringReader(it) } }
        var currentGroup = SourceMapGroup().also { sourceMap.groups += it }

        while (!stream.isEof) {
            if (stream.isGroupTerminator) {
                currentGroup = SourceMapGroup().also { sourceMap.groups += it }
                jsColumn = 0
                stream.skipChar()
                continue
            }

            jsColumn += stream.readInt() ?: return stream.createError("VLQ-encoded JS column number expected")

            if (stream.isEncodedInt) {
                sourceIndex += stream.readInt() ?: return stream.createError("VLQ-encoded source index expected")
                sourceLine += stream.readInt() ?: return stream.createError("VLQ-encoded source line expected")
                sourceColumn += stream.readInt() ?: return stream.createError("VLQ-encoded source column expected")
                val name = if (stream.isEncodedInt) {
                    nameIndex += stream.readInt() ?: return stream.createError("VLQ-encoded name index expected")
                    if (nameIndex !in names.indices) {
                        return stream.createError("Name index $nameIndex is out of bounds ${names.indices}")
                    }
                    names[nameIndex]
                } else null

                if (sourceIndex !in sources.indices) {
                    return stream.createError("Source index $sourceIndex is out of bounds ${sources.indices}")
                }
                currentGroup.segments += SourceMapSegment(jsColumn, sourceRoot + sources[sourceIndex], sourceLine, sourceColumn, name)
            } else {
                currentGroup.segments += SourceMapSegment(jsColumn, null, -1, -1, null)
            }

            when {
                stream.isEof -> break
                stream.isGroupTerminator -> {
                    currentGroup = SourceMapGroup().also { sourceMap.groups += it }
                    jsColumn = 0
                }
                !stream.isSegmentTerminator -> return stream.createError("Unexpected char, ',' or ';' expected")
            }
            stream.skipChar()
        }

        return SourceMapSuccess(sourceMap)
    }

    internal class MappingStream(val string: String) {
        var position = 0

        val isEof: Boolean get() = position == string.length

        val isSegmentTerminator: Boolean get() = string[position] == ','

        val isGroupTerminator: Boolean get() = string[position] == ';'

        val isEncodedInt: Boolean get() = !isEof && !isSegmentTerminator && !isGroupTerminator

        fun skipChar() {
            position++
        }

        fun readInt(): Int? {
            var value = 0
            var shift = 0
            while (true) {
                if (isEof) return null
                val digit = base64value(string[position++]) ?: return null

                val digitValue = digit and 0x1F
                value = value or (digitValue shl shift)

                if ((digit and 0x20) == 0) break
                shift += 5
            }

            val unsignedValue = value ushr 1
            return if ((value and 1) == 0) unsignedValue else -unsignedValue
        }

        private fun base64value(c: Char): Int? = when (c) {
            in 'A'..'Z' -> c.toInt() - 'A'.toInt()
            in 'a'..'z' -> c.toInt() - 'a'.toInt() + 26
            in '0'..'9' -> c.toInt() - '0'.toInt() + 52
            '+' -> 62
            '/' -> 63
            else -> null
        }

        fun createError(error: String): SourceMapError = SourceMapError("Error parsing stream at offset $position: $error")
    }
}
