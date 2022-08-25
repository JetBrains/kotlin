/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.util.prefixBaseNameIfNot
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.util.suffixIfNot
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import kotlin.random.Random

sealed class AbstractOutputs(
        val target: KonanTarget,
        val produce: CompilerOutputKind,
) {
    protected val prefix = produce.prefix(target)
    protected val suffix = produce.suffix(target)

    protected fun String.fullOutputName() = prefixBaseNameIfNeeded(prefix).suffixIfNeeded(suffix)

    private fun String.prefixBaseNameIfNeeded(prefix: String) =
            if (produce.isCache)
                prefixBaseNameAlways(prefix)
            else prefixBaseNameIfNot(prefix)

    private fun String.suffixIfNeeded(prefix: String) =
            if (produce.isCache)
                suffixAlways(prefix)
            else suffixIfNot(prefix)

    private fun String.prefixBaseNameAlways(prefix: String): String {
        val file = File(this).absoluteFile
        val name = file.name
        val directory = file.parent
        return "$directory/$prefix$name"
    }

    private fun String.suffixAlways(suffix: String) = "$this$suffix"
}

class CacheOutputs(
        outputPath: String?,
        target: KonanTarget,
        produce: CompilerOutputKind,
        producePerFileCache: Boolean,
) : AbstractOutputs(target, produce) {

    init {
        require(produce.isCache)
    }

    val outputName = outputPath?.removeSuffixIfPresent(suffix) ?: produce.visibleName

    val mainFile = File(outputName)

    private val pathToPerFileCache =
            if (producePerFileCache)
                outputName.substring(0, outputName.lastIndexOf(File.separatorChar) /* skip [PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME]*/)
            else null

    val perFileCacheFileName = pathToPerFileCache?.let { File(it).name }

    val cacheFileName = File((pathToPerFileCache ?: outputName).fullOutputName()).absoluteFile.name

    val tempCacheDirectory: File = File(outputName + Random.nextLong().toString())

    val bitcodeDependenciesFile = tempCacheDirectory.child(CachedLibraries.BITCODE_DEPENDENCIES_FILE_NAME)

    val inlineFunctionBodiesFile = tempCacheDirectory.child(CachedLibraries.INLINE_FUNCTION_BODIES_FILE_NAME)

    val classFieldsFile = tempCacheDirectory.child(CachedLibraries.CLASS_FIELDS_FILE_NAME)
}

class CLibraryOutputs(
        outputPath: String?,
        target: KonanTarget,
        produce: CompilerOutputKind,
) : AbstractOutputs(target, produce) {
    init {
        require(produce == CompilerOutputKind.STATIC || produce == CompilerOutputKind.DYNAMIC)
    }

    val outputName = outputPath?.removeSuffixIfPresent(suffix) ?: produce.visibleName

    /**
     * Header file for C library.
     */
    val cAdapterHeader by lazy { File("${outputName}_api.h") }
    val cAdapterDef by lazy { File("${outputName}.def") }
}

class KlibOutputs(
        outputPath: String?,
        target: KonanTarget
) : AbstractOutputs(target, CompilerOutputKind.LIBRARY) {

    val outputName = outputPath?.removeSuffixIfPresent(suffix) ?: produce.visibleName

    fun klibOutputFileName(isPacked: Boolean): String =
            if (isPacked) "$outputName$suffix" else outputName
}

class FrameworkOutputs(
        private val outputPath: String?,
        target: KonanTarget
) : AbstractOutputs(target, CompilerOutputKind.FRAMEWORK) {
    fun directoryForFramework(frameworkName: String): File {
        val dir = File(outputPath!!).parentFile
        val name = if (!frameworkName.endsWith(".framework")) {
            frameworkName + ".framework"
        } else {
            frameworkName
        }
        return dir.child(name)
    }
}

/**
 * Creates and stores terminal compiler outputs.
 */
class OutputFiles(
        outputPath: String?,
        target: KonanTarget,
        produce: CompilerOutputKind,
        producePerFileCache: Boolean,
) : AbstractOutputs(target, produce) {

    val outputName = outputPath?.removeSuffixIfPresent(suffix) ?: produce.visibleName

    fun klibOutputFileName(isPacked: Boolean): String =
            if (isPacked) "$outputName$suffix" else outputName

    /**
     * Header file for dynamic library.
     */
    val cAdapterHeader by lazy { File("${outputName}_api.h") }
    val cAdapterDef by lazy { File("${outputName}.def") }

    /**
     * Compiler's main output file.
     */
    val mainFileName =
            if (produce.isCache)
                outputName
            else
                outputName.fullOutputName()

    val mainFile = File(mainFileName)

    private val pathToPerFileCache =
            if (producePerFileCache)
                outputName.substring(0, outputName.lastIndexOf(File.separatorChar) /* skip [PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME]*/)
            else null

    val perFileCacheFileName = pathToPerFileCache?.let { File(it).name }

    val cacheFileName = File((pathToPerFileCache ?: outputName).fullOutputName()).absoluteFile.name

    val dynamicCacheInstallName = File(outputName).child(cacheFileName).absolutePath

    val tempCacheDirectory =
            if (produce.isCache)
                File(outputName + Random.nextLong().toString())
            else null

    val nativeBinaryFile = tempCacheDirectory?.child(cacheFileName)?.absolutePath ?: mainFileName

    val symbolicInfoFile = "$nativeBinaryFile.dSYM"

    val bitcodeDependenciesFile = tempCacheDirectory?.child(CachedLibraries.BITCODE_DEPENDENCIES_FILE_NAME)

    val inlineFunctionBodiesFile = tempCacheDirectory?.child(CachedLibraries.INLINE_FUNCTION_BODIES_FILE_NAME)

    val classFieldsFile = tempCacheDirectory?.child(CachedLibraries.CLASS_FIELDS_FILE_NAME)
}