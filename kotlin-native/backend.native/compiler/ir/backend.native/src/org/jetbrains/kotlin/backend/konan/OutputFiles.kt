/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.util.prefixBaseNameIfNot
import org.jetbrains.kotlin.util.suffixIfNot
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import kotlin.random.Random

private class OutputFilesMangler(target: KonanTarget, private val produce: CompilerOutputKind) {
    val prefix = produce.prefix(target)
    val suffix = produce.suffix(target)

    fun String.fullOutputName() = prefixBaseNameIfNeeded(prefix).suffixIfNeeded(suffix)

    private fun String.prefixBaseNameIfNeeded(prefix: String) =
            if (produce.isCache)
                prefixBaseNameAlways(prefix)
            else prefixBaseNameIfNot(prefix)

    private fun String.suffixAlways(suffix: String) = "$this$suffix"

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
}

class CacheOutputs(val cacheRootPath: String, target: KonanTarget, private val produce: CompilerOutputKind) {

    private val mangler = OutputFilesMangler(target, produce)

    val perFileCacheFileName = File(cacheRootPath).absoluteFile.name

    val tempCacheDirectory = File(cacheRootPath + Random.nextLong().toString())

    val bitcodeDependenciesFile = File(tempCacheDirectory.cacheBinaryPart(), CachedLibraries.BITCODE_DEPENDENCIES_FILE_NAME)
    val inlineFunctionBodiesFile = File(tempCacheDirectory.cacheIrPart(), CachedLibraries.INLINE_FUNCTION_BODIES_FILE_NAME)
    val classFieldsFile = File(tempCacheDirectory.cacheIrPart(), CachedLibraries.CLASS_FIELDS_FILE_NAME)
    val eagerInitializedPropertiesFile = File(tempCacheDirectory.cacheIrPart(), CachedLibraries.EAGER_INITIALIZED_PROPERTIES_FILE_NAME)

    private fun File.cacheBinaryPart() = File(this, CachedLibraries.PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)

    private fun File.cacheIrPart() = File(this, CachedLibraries.PER_FILE_CACHE_IR_LEVEL_DIR_NAME)

    val cacheFileName = with(mangler) { File((cacheRootPath).fullOutputName()).absoluteFile.name }


    val dynamicCacheInstallName = File(File(cacheRootPath).cacheBinaryPart(), cacheFileName).absolutePath

    fun prepareTempDirectories() {
        tempCacheDirectory.mkdirs()
        tempCacheDirectory.cacheBinaryPart().mkdirs()
        tempCacheDirectory.cacheIrPart().mkdirs()
    }
}

class KlibOutputFiles(private val outputName: String, target: KonanTarget, val produce: CompilerOutputKind) {
    private val mangler = OutputFilesMangler(target, produce)

    fun klibOutputFileName(isPacked: Boolean): String =
            with(mangler) { if (isPacked) "$outputName$suffix" else outputName }
}

/**
 * Creates and stores terminal compiler outputs.
 */
class OutputFiles(val outputName: String, target: KonanTarget, val produce: CompilerOutputKind) {

    private val mangler = OutputFilesMangler(target, produce)

    /**
     * Compiler's main output file.
     */
    val mainFileName = with(mangler) { outputName.fullOutputName() }

    val mainFile = File(mainFileName)

    val perFileCacheFileName = File(outputName).absoluteFile.name
}