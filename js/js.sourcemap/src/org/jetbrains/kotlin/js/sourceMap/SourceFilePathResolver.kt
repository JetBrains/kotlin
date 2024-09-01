/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.sourceMap

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import java.io.IOException

class SourceFilePathResolver(
    sourceRoots: List<File>,
    outputDir: File? = null,
    private val includeUnavailableSourcesIntoSourceMap: Boolean = false
) {
    private val sourceRoots = sourceRoots.mapTo(mutableSetOf<File>()) { it.absoluteFile }
    private val outputDirPathResolver = outputDir?.let(::RelativePathCalculator)
    private val cache = mutableMapOf<File, String>()
    private val modulesAndTheirSourcesStatus = hashMapOf<String, Boolean>()

    @Throws(IOException::class)
    fun getPathRelativeToSourceRoots(file: File): String {
        var path = cache[file]
        if (path == null) {
            path = calculatePathRelativeToSourceRoots(file)
            cache[file] = path
        }
        return path
    }

    @Throws(IOException::class)
    fun getPathRelativeToSourceRootsIfExists(moduleId: String, file: File): String? {
        val moduleSourcesShouldBeAdded = includeUnavailableSourcesIntoSourceMap || modulesAndTheirSourcesStatus.getOrPut(moduleId) { file.exists() }
        return runIf(moduleSourcesShouldBeAdded) { getPathRelativeToSourceRoots(file) }
    }

    @Throws(IOException::class)
    private fun calculatePathRelativeToSourceRoots(file: File): String {
        val pathRelativeToOutput = calculatePathRelativeToOutput(file)
        if (pathRelativeToOutput != null) return pathRelativeToOutput
        val parts = mutableListOf<String>()
        var currentFile: File? = file.absoluteFile.normalize()
        while (currentFile != null) {
            if (sourceRoots.contains(currentFile)) {
                if (parts.isEmpty()) {
                    break
                }
                parts.reverse()
                return parts.joinToString(File.separator)
            }
            parts.add(currentFile.name)
            currentFile = currentFile.parentFile
        }
        return file.name
    }

    private fun calculatePathRelativeToOutput(file: File): String? {
        return outputDirPathResolver?.calculateRelativePathTo(file)
    }

    companion object {
        @JvmStatic
        fun create(config: JsConfig) = create(config.configuration)

        @JvmStatic
        fun create(configuration: CompilerConfiguration) = create(
            sourceRoots = configuration.get(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, emptyList()),
            sourceMapPrefix = configuration.get(JSConfigurationKeys.SOURCE_MAP_PREFIX, ""),
            outputDir = configuration.get(JSConfigurationKeys.OUTPUT_DIR),
            includeUnavailableSourcesIntoSourceMap = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES)
        )

        @JvmStatic
        fun create(
            sourceRoots: List<String>,
            sourceMapPrefix: String,
            outputDir: File?,
            includeUnavailableSourcesIntoSourceMap: Boolean = false,
        ): SourceFilePathResolver {
            val generateRelativePathsInSourceMap = sourceMapPrefix.isEmpty() && sourceRoots.isEmpty()
            return SourceFilePathResolver(
                sourceRoots.map(::File),
                outputDir.takeIf { generateRelativePathsInSourceMap },
                includeUnavailableSourcesIntoSourceMap
            )
        }
    }
}
