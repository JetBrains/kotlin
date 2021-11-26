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


/**
 * Creates and stores terminal compiler outputs.
 */
class OutputFiles(outputPath: String?, target: KonanTarget, val produce: CompilerOutputKind) {

    private val prefix = produce.prefix(target)
    private val suffix = produce.suffix(target)

    val outputName = outputPath?.removeSuffixIfPresent(suffix) ?: produce.visibleName

    fun klibOutputFileName(isPacked: Boolean): String =
            if (isPacked) "$outputName$suffix" else outputName

    /**
     * Header file for dynamic library.
     */
    val cAdapterHeader by lazy { File("${outputName}_api.h") }
    val cAdapterDef    by lazy { File("${outputName}.def") }

    /**
     * Main compiler's output file.
     */
    val mainFile =
            if (produce.isCache)
                outputName
            else
                outputName.fullOutputName()

    private val cacheFile = File(outputName.fullOutputName()).absoluteFile.name

    val dynamicCacheInstallName = File(outputName).child(cacheFile).absolutePath

    val tempCacheDirectory =
            if (produce.isCache)
                File(outputName + Random.nextLong().toString())
            else null

    val nativeBinaryFile =
            if (produce.isCache)
                tempCacheDirectory!!.child(cacheFile).absolutePath
            else mainFile

    val symbolicInfoFile = "$nativeBinaryFile.dSYM"

    val bitcodeDependenciesFile =
            if (produce.isCache)
                tempCacheDirectory!!.child(CachedLibraries.BITCODE_DEPENDENCIES_FILE_NAME).absolutePath
            else null

    val inlineFunctionBodiesFile =
            if (produce.isCache)
                tempCacheDirectory!!.child(CachedLibraries.INLINE_FUNCTION_BODIES_FILE_NAME).absolutePath
            else null

    val classFieldsFile =
            if (produce.isCache)
                tempCacheDirectory!!.child(CachedLibraries.CLASS_FIELDS_FILE_NAME).absolutePath
            else null

    private fun String.fullOutputName() = prefixBaseNameIfNeeded(prefix).suffixIfNeeded(suffix)

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