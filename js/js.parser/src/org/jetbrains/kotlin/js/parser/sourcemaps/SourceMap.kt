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

class SourceMap(val sourceContentResolver: (String) -> Reader?) {
    val groups = mutableListOf<SourceMapGroup>()

    @Suppress("UNUSED") // For use in the debugger
    fun debugToString() = ByteArrayOutputStream().also { debug(PrintStream(it)) }.toString()

    fun debug(writer: PrintStream = System.out) {
        for ((index, group) in groups.withIndex()) {
            writer.print("${index + 1}:")
            for (segment in group.segments) {
                val nameIfPresent = if (segment.name != null) "(${segment.name})" else ""
                writer.print(" ${segment.generatedColumnNumber + 1}:${segment.sourceLineNumber + 1},${segment.sourceColumnNumber + 1}$nameIfPresent")
            }
            writer.println()
        }
    }

    fun debugVerbose(writer: PrintStream, generatedJsFile: File) {
        assert(generatedJsFile.exists()) { "$generatedJsFile does not exist!" }
        val generatedLines = generatedJsFile.readLines().toTypedArray()
        for ((index, group) in groups.withIndex()) {
            writer.print("${index + 1}:")
            val generatedLine = generatedLines[index]
            val segmentsByColumn = group.segments.map { it.generatedColumnNumber to it }.toMap()
            for (i in generatedLine.indices) {
                segmentsByColumn[i]?.let { (_, sourceFile, sourceLine, sourceColumn, name) ->
                    val nameIfPresent = if (name != null) "($name)" else ""
                    writer.print("<$sourceFile:${sourceLine + 1}:${sourceColumn + 1}$nameIfPresent>")
                }
                writer.print(generatedLine[i])
            }
            writer.println()
        }
    }

    fun segmentForGeneratedLocation(lineNumber: Int, columnNumber: Int?): SourceMapSegment? {
        val group = groups.getOrNull(lineNumber)?.takeIf { it.segments.isNotEmpty() } ?: return null
        return if (columnNumber == null || columnNumber <= group.segments[0].generatedColumnNumber) {
            group.segments[0]
        } else {
            val candidateIndex = group.segments.indexOfFirst {
                columnNumber <= it.generatedColumnNumber
            }
            if (candidateIndex < 0)
                null
            else if (candidateIndex == 0 || group.segments[candidateIndex].generatedColumnNumber == columnNumber)
                group.segments[candidateIndex]
            else
                group.segments[candidateIndex - 1]
        }
    }

    companion object {
        @Throws(IOException::class, SourceMapSourceReplacementException::class)
        fun replaceSources(sourceMapFile: File, mapping: (String) -> String): Boolean {
            val content = sourceMapFile.readText()
            return sourceMapFile.writer().buffered().use {
                mapSources(content, it, mapping)
            }
        }

        @Throws(IOException::class, SourceMapSourceReplacementException::class)
        fun mapSources(content: String, output: Writer, mapping: (String) -> String): Boolean {
            val json = try {
                parseJson(content)
            } catch (e: JsonSyntaxException) {
                throw SourceMapSourceReplacementException(cause = e)
            }
            val jsonObject = json as? JsonObject ?: throw SourceMapSourceReplacementException("Top-level object expected")
            val sources = jsonObject.properties["sources"]
            if (sources != null) {
                val sourcesArray =
                    sources as? JsonArray ?: throw SourceMapSourceReplacementException("'sources' property is not of array type")
                var changed = false
                val fixedSources = sourcesArray.elements.mapTo(mutableListOf<JsonNode>()) {
                    val sourcePath = it as? JsonString ?: throw SourceMapSourceReplacementException("'sources' array must contain strings")
                    val replacedPath = mapping(sourcePath.value)
                    if (!changed && replacedPath != sourcePath.value) {
                        changed = true
                    }
                    JsonString(replacedPath)
                }
                if (!changed) return false
                jsonObject.properties["sources"] = JsonArray(fixedSources)
            }
            jsonObject.write(output)
            return true
        }
    }
}

class SourceMapSourceReplacementException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

data class SourceMapSegment(
    val generatedColumnNumber: Int,
    val sourceFileName: String?,
    val sourceLineNumber: Int,
    val sourceColumnNumber: Int,
    val name: String?,
)

class SourceMapGroup {
    val segments = mutableListOf<SourceMapSegment>()
}

sealed class SourceMapParseResult

class SourceMapSuccess(val value: SourceMap) : SourceMapParseResult()

class SourceMapError(val message: String) : SourceMapParseResult()
