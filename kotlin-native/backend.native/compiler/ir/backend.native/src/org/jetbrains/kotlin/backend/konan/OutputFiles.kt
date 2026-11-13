/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.io.listDirectoryEntriesIfDirectoryExists
import org.jetbrains.kotlin.util.prefixBaseNameIfNot
import org.jetbrains.kotlin.util.suffixIfNot
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.random.Random

/**
 * Creates and stores terminal compiler outputs.
 */
class OutputFiles(val outputName: String, target: KonanTarget, val produce: CompilerOutputKind) {

    private val prefix = produce.prefix(target)
    private val suffix = produce.suffix(target)

    /**
     * Header file for dynamic library.
     */
    val cAdapterHeader by lazy { Path("${outputName}_api.h") }
    val cAdapterDef    by lazy { Path("${outputName}.def") }

    /**
     * Compiler's main output file.
     */
    val mainFileName =
            if (produce.isCache)
                outputName
            else
                outputName.fullOutputName()

    val mainFile = Path(mainFileName)

    val perFileCacheFileName = Path(outputName).absolute().name

    val cacheFileName = Path((outputName).fullOutputName()).absolute().name

    private fun Path.cacheBinaryPart() = this.resolve(CachedLibraries.PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)

    private fun Path.cacheIrPart() = this.resolve(CachedLibraries.PER_FILE_CACHE_IR_LEVEL_DIR_NAME)

    val dynamicCacheInstallName = Path(outputName).cacheBinaryPart().resolve(cacheFileName).absolutePathString()

    val tempCacheDirectory =
            if (produce.isCache)
                Path(outputName + Random.nextLong().toString())
            else null

    fun prepareTempDirectories() {
        tempCacheDirectory?.createDirectories()
        tempCacheDirectory?.cacheBinaryPart()?.createDirectories()
        tempCacheDirectory?.cacheIrPart()?.createDirectories()
    }

    val nativeBinaryFile = tempCacheDirectory?.cacheBinaryPart()?.resolve(cacheFileName)?.absolutePathString() ?: mainFileName

    /**
     * Whether the already existing cache directory holds nothing but the metadata-only (header) artifact
     * of the library, i.e. whether a full cache being finalized can upgrade it in place
     * (see [CacheStorage.renameOutput]).
     */
    internal fun containsOnlyHeaderCacheArtifact(): Boolean {
        val binaryPartEntries = mainFile.resolve(CachedLibraries.PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)
                .listDirectoryEntriesIfDirectoryExists()
                .map { it.name }
        val headerArtifactName = "$perFileCacheFileName.header"
        // The header cache records its bitcode dependencies next to the placeholder artifact,
        // and a full cache additionally carries the real binary - anything but the placeholder
        // and the dependency file means the existing cache is not metadata-only.
        return headerArtifactName in binaryPartEntries &&
                binaryPartEntries.all { it == headerArtifactName || it == CachedLibraries.BITCODE_DEPENDENCIES_FILE_NAME }
    }

    val symbolicInfoFile = "$nativeBinaryFile.dSYM"

    val cacheMetadata = tempCacheDirectory?.resolve(CachedLibraries.METADATA_FILE_NAME)

    val bitcodeDependenciesFile = tempCacheDirectory?.cacheBinaryPart()?.resolve(CachedLibraries.BITCODE_DEPENDENCIES_FILE_NAME)

    val inlineFunctionBodiesFile = tempCacheDirectory?.cacheIrPart()?.resolve(CachedLibraries.INLINE_FUNCTION_BODIES_FILE_NAME)

    val classFieldsFile = tempCacheDirectory?.cacheIrPart()?.resolve(CachedLibraries.CLASS_FIELDS_FILE_NAME)

    val eagerInitializedPropertiesFile = tempCacheDirectory?.cacheIrPart()?.resolve(CachedLibraries.EAGER_INITIALIZED_PROPERTIES_FILE_NAME)

    val trivialGettersFile = tempCacheDirectory?.cacheIrPart()?.resolve(CachedLibraries.TRIVIAL_GETTERS_FILE_NAME)

    val objCAdaptersFile = tempCacheDirectory?.cacheIrPart()?.resolve(CachedLibraries.OBJC_ADAPTERS_FILE_NAME)

    val partialLinkageIssuesFile = tempCacheDirectory?.cacheIrPart()?.resolve(CachedLibraries.PARTIAL_LINKAGE_ISSUES_FILE_NAME)

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
        val absolutePath: Path = Path(this).absolute()
        val name: String = absolutePath.name
        val directory: Path = absolutePath.parent
        return directory.resolve("$prefix$name").pathString
    }

    private fun String.suffixAlways(suffix: String) = "$this$suffix"
}