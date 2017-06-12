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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.io.Reader

object SourceMapParser {
    @Throws(IOException::class)
    fun parse(reader: Reader): SourceMapParseResult {
        val jsonObject = try {
            JSONObject(JSONTokener(reader))
        }
        catch (e: JSONException) {
            return SourceMapError(e.message ?: "parse error")
        }

        if (!jsonObject.has("version")) return SourceMapError("Version not defined")
        jsonObject.get("version").let {
            if (it != 3) return SourceMapError("Unsupported version: $it")
        }

        val sourceRoot = if (jsonObject.has("sourceRoot")) {
            jsonObject.get("sourceRoot") as? String ?: return SourceMapError("'sourceRoot' property is not of string type")
        }
        else {
            ""
        }

        val sources = if (jsonObject.has("sources")) {
            val sourcesProperty = jsonObject.get("sources") as? JSONArray ?:
                                  return SourceMapError("'sources' property is not of array type")
            sourcesProperty.map {
                it as? String ?: return SourceMapError("'sources' array must contain strings")
            }
        }
        else {
            emptyList()
        }

        if (!jsonObject.has("mappings")) return SourceMapError("'mappings' property not found")
        val mappings = jsonObject.get("mappings") as? String ?: return SourceMapError("'mappings' property is not of string type")

        var jsColumn = 0
        var sourceLine = 0
        var sourceColumn = 0
        var sourceIndex = 0
        val stream = MappingStream(mappings)
        val sourceMap = SourceMap()
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
                if (stream.isEncodedInt) {
                    stream.readInt() ?: return stream.createError("VLQ-encoded name index expected")
                }
            }

            if (sourceIndex !in sources.indices) {
                return stream.createError("Source index $sourceIndex is out of bounds ${sources.indices}")
            }

            currentGroup.segments += SourceMapSegment(jsColumn, sourceRoot + sources[sourceIndex], sourceLine, sourceColumn)

            when {
                stream.isEof -> return stream.createError("Unexpected EOF, ',' or ';' expected")
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
