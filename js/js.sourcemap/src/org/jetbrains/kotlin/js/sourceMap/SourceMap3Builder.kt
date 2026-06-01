/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.sourceMap

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.jetbrains.kotlin.js.backend.ast.JsLocation
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import java.io.File
import java.io.IOException
import java.io.Reader
import java.util.function.Supplier

class SourceMap3Builder(
    private val generatedFile: File?,
    private val getCurrentOutputColumn: () -> Int,
    private val pathPrefix: String
) : SourceMapMappingConsumer {

    private val out = StringBuilder(8192)

    private val sources = createOpenHashMap<SourceKey>()
    private val orderedSources = mutableListOf<String>()
    private val orderedSourceContentSuppliers = mutableListOf<Supplier<Reader?>>()
    private val ignoredSources = linkedSetOf<Int>()

    private val names = createOpenHashMap<String>()
    private val orderedNames = mutableListOf<String>()
    private var previousNameIndex = 0
    private var previousPreviousNameIndex = 0

    private var previousGeneratedColumn = -1
    private var previousSourceIndex = 0
    private var previousSourceLine = 0
    private var previousSourceColumn = 0
    private var previousMappingOffset = 0
    private var previousPreviousSourceIndex = 0
    private var previousPreviousSourceLine = 0
    private var previousPreviousSourceColumn = 0
    private var currentMappingIsEmpty = true

    fun build(): String {
        val json = JsonObject()
        json.properties["version"] = JsonNumber(3.0)
        if (generatedFile != null)
            json.properties["file"] = JsonString(generatedFile.name)
        appendSources(json)
        appendSourcesContent(json)
        appendIgnoredSources(json)
        json.properties["names"] = JsonArray(
            orderedNames.mapTo(mutableListOf()) { JsonString(it) }
        )
        json.properties["mappings"] = JsonString(out.toString())
        return json.toString()
    }

    private fun appendSources(json: JsonObject) {
        val prefixedPaths = orderedSources.map { pathPrefix + it }
        val [sourceRoot, paths] = calculateCommonPathPrefix(prefixedPaths)

        if (sourceRoot != null) {
            json.properties["sourceRoot"] = JsonString(sourceRoot)
        }

        json.properties["sources"] = JsonArray(
            paths.mapTo(mutableListOf()) { JsonString(it) }
        )
    }

    private fun appendIgnoredSources(json: JsonObject) {
        val ignoreList = JsonArray(ignoredSources.mapTo(mutableListOf()) { JsonNumber(it.toDouble()) })

        json.properties["ignoreList"] = ignoreList
        json.properties["x_google_ignoreList"] = ignoreList
    }

    private fun appendSourcesContent(json: JsonObject) {
        json.properties["sourcesContent"] = JsonArray(
            orderedSourceContentSuppliers.mapTo(mutableListOf()) {
                try {
                    it.get().use { reader ->
                        if (reader != null)
                            JsonString(reader.readText())
                        else
                            JsonNull
                    }
                } catch (e: IOException) {
                    System.err.println("An exception occurred during embedding sources into source map")
                    e.printStackTrace()
                    // can't close the content reader or read from it
                    JsonNull
                }
            }
        )
    }

    override fun newLine() {
        out.append(';')
        previousGeneratedColumn = -1
    }

    private fun getSourceIndex(source: String, fileIdentity: Any?, contentSupplier: Supplier<Reader?>): Int {
        val key = SourceKey(source, fileIdentity)
        var sourceIndex = sources.getInt(key)
        if (sourceIndex == -1) {
            sourceIndex = orderedSources.size
            sources.put(key, sourceIndex)
            orderedSources.add(source)
            orderedSourceContentSuppliers.add(contentSupplier)
        }
        return sourceIndex
    }

    private fun getNameIndex(name: String): Int {
        var nameIndex = names.getInt(name)
        if (nameIndex == -1) {
            nameIndex = orderedNames.size
            names.put(name, nameIndex)
            orderedNames.add(name)
        }
        return nameIndex
    }

    private fun calculateCommonPathPrefix(paths: List<String>): Pair<String?, List<String>> {
        // Special handling for [JsLocation.IGNORED] as it is virtual file without a real path,
        // it should not participate in sourceRoot calculation and should be kept as-is.
        fun String.shouldKeepOriginalPrefix() =
            this == JsLocation.IGNORED.file

        /**
         * Calculates the length of the common directory prefix for given Unix-style paths,
         * including the trailing '/' separator.
         *
         * Returns 0 if:
         * - Paths list is empty or contains less than 2 entries
         * - There is no common directory prefix, i.e. paths have no shared '/'-separated segments
         *
         * Example:
         * For paths
         * ```
         * foo/bar.kt
         * foo/bar/a.kt
         * ```
         * Returns 4 (the length of 'foo/').
         */
        fun commonUnixPathPrefixLength(paths: List<String>): Int {
            // There is no sense in calculating common parent for the single path, we will save it as is
            if (paths.size < 2) return 0

            // The idea is to find common path between least common paths - first one and last one of sorted paths list.
            val first = paths.max()
            val last = paths.min()

            val [shorter, longer] = if (first.length < last.length) first to last else last to first

            var latestSeparatorIndex = -1

            for (i in shorter.indices) {
                if (shorter[i] == '/') latestSeparatorIndex = i
                if (shorter[i] != longer[i]) break
            }

            return latestSeparatorIndex + 1
        }

        val applicablePaths = paths.filter { !it.shouldKeepOriginalPrefix() }
        val sourceRootLength = commonUnixPathPrefixLength(applicablePaths)
        if (sourceRootLength == 0) return null to paths

        // Common prefix should contain the leading '/', so upper index also includes it
        val commonPrefix = applicablePaths
            .first()
            .substring(0, sourceRootLength)

        return commonPrefix to paths.map {
            if (it.shouldKeepOriginalPrefix()) it
            else it.substring(sourceRootLength)
        }
    }

    private val String.unixStylePath: String
        get() = replace(File.separatorChar, '/')

    fun addIgnoredSource(
        source: String,
        fileIdentity: Any? = null,
        sourceContent: Supplier<Reader?> = Supplier { null },
    ) {
        ignoredSources.add(getSourceIndex(source.unixStylePath, fileIdentity, sourceContent))
    }

    override fun addMapping(
        source: String,
        fileIdentity: Any?,
        sourceContent: Supplier<Reader?>,
        sourceLine: Int,
        sourceColumn: Int,
        name: String?,
    ) {
        addMapping(source, sourceLine, sourceColumn, getCurrentOutputColumn(), name, fileIdentity, sourceContent)
    }

    fun addMapping(
        source: String,
        sourceLine: Int,
        sourceColumn: Int,
        outputColumn: Int,
        name: String? = null,
        fileIdentity: Any? = null,
        sourceContent: Supplier<Reader?> = Supplier { null },
    ) {
        val sourceIndex = getSourceIndex(source.unixStylePath, fileIdentity, sourceContent)

        val nameIndex = name?.let(this::getNameIndex) ?: -1

        if (!currentMappingIsEmpty &&
            source != JsLocation.IGNORED.file &&
            previousSourceIndex == sourceIndex &&
            previousSourceLine == sourceLine &&
            previousSourceColumn == sourceColumn
        ) {
            return
        }

        startMapping(outputColumn)

        Base64VLQ.encode(out, sourceIndex - previousSourceIndex)
        previousSourceIndex = sourceIndex

        Base64VLQ.encode(out, sourceLine - previousSourceLine)
        previousSourceLine = sourceLine

        Base64VLQ.encode(out, sourceColumn - previousSourceColumn)
        previousSourceColumn = sourceColumn

        if (nameIndex >= 0) {
            Base64VLQ.encode(out, nameIndex - previousNameIndex)
            previousNameIndex = nameIndex
        }

        currentMappingIsEmpty = false
    }

    override fun addEmptyMapping() {
        if (!currentMappingIsEmpty) {
            startMapping(getCurrentOutputColumn())
            currentMappingIsEmpty = true
        }
    }

    private fun startMapping(column: Int) {
        val newGroupStarted = previousGeneratedColumn == -1
        if (newGroupStarted) {
            previousGeneratedColumn = 0
        }

        val columnDiff = column - previousGeneratedColumn
        if (!newGroupStarted) {
            out.append(',')
        }
        if (columnDiff > 0 || newGroupStarted) {
            Base64VLQ.encode(out, columnDiff)
            previousGeneratedColumn = column

            previousMappingOffset = out.length
            previousPreviousSourceIndex = previousSourceIndex
            previousPreviousSourceLine = previousSourceLine
            previousPreviousSourceColumn = previousSourceColumn
            previousPreviousNameIndex = previousNameIndex
        } else {
            out.setLength(previousMappingOffset)
            previousSourceIndex = previousPreviousSourceIndex
            previousSourceLine = previousPreviousSourceLine
            previousSourceColumn = previousPreviousSourceColumn
            previousNameIndex = previousPreviousNameIndex
        }
    }

    private object Base64VLQ {
        // A Base64 VLQ digit can represent 5 bits, so it is base-32.
        private const val VLQ_BASE_SHIFT = 5
        private const val VLQ_BASE = 1 shl VLQ_BASE_SHIFT

        // A mask of bits for a VLQ digit (11111), 31 decimal.
        private const val VLQ_BASE_MASK = VLQ_BASE - 1

        // The continuation bit is the 6th bit.
        private const val VLQ_CONTINUATION_BIT = VLQ_BASE

        @Suppress("SpellCheckingInspection")
        private val BASE64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()

        private fun toVLQSigned(value: Int) =
            if (value < 0) (-value shl 1) + 1 else value shl 1

        fun encode(out: StringBuilder, value: Int) {
            @Suppress("NAME_SHADOWING")
            var value = toVLQSigned(value)
            do {
                var digit = value and VLQ_BASE_MASK
                value = value ushr VLQ_BASE_SHIFT
                if (value > 0) {
                    digit = digit or VLQ_CONTINUATION_BIT
                }
                out.append(BASE64_MAP[digit])
            } while (value > 0)
        }
    }

    private data class SourceKey(
        private val sourcePath: String,
        /**
         * An object to distinguish different files with the same paths
         */
        private val fileIdentity: Any?
    )

    private fun <T> createOpenHashMap() = Object2IntOpenHashMap<T>().apply {
        defaultReturnValue(-1)
    }
}
