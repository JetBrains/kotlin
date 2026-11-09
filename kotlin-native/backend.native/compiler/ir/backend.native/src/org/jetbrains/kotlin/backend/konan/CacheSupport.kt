/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.konan.library.KlibDAG
import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.backend.konan.serialization.KonanPartialModuleDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.PartialCacheInfo
import org.jetbrains.kotlin.backend.konan.util.reportCompilationErrorAndThrow
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.config.filesToCache
import org.jetbrains.kotlin.konan.config.konanLibraryToAddToCache
import org.jetbrains.kotlin.konan.config.makePerFileCache
import org.jetbrains.kotlin.konan.config.preLinkCaches
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.irOrFail
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.WireFormat
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import org.jetbrains.kotlin.backend.common.serialization.proto.FileEntry as ProtoFileEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

data class FileWithFqName(val filePath: String, val fqName: String)

/**
 * The paths and package names of the files of this library.
 * This runs for each library with a per-file cache on every compilation, so only the few needed fields of each file are read:
 * a full parse would also read e.g. the line offsets of each file entry.
 */
fun KotlinLibrary.getFilesWithFqNames(): List<FileWithFqName> {
    val ir = irOrFail
    return List(ir.irFileCount) { ir.readFileWithFqName(it) }
}

fun KotlinLibrary.getFileFqNames(filePaths: List<String>): List<String> {
    val fqNames = getFilesWithFqNames().associate { (filePath, fqName) -> filePath to fqName }
    return filePaths.map { fqNames[it] ?: error("No file with path $it is found in klib $path") }
}

/** Reads the [ProtoFile] fields `file_entry`, `file_entry_id` and `fq_name`, skipping the other ones. */
private fun KlibIrComponent.readFileWithFqName(fileIndex: Int): FileWithFqName {
    val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(this, fileIndex))
    val input = irFile(fileIndex).codedInputStream
    var inlineFileEntryName: String? = null
    var fileEntryId: Int? = null
    val fqName = mutableListOf<Int>()
    while (true) {
        val tag = input.readTag()
        if (tag == 0) break
        when (WireFormat.getTagFieldNumber(tag)) {
            ProtoFile.FILE_ENTRY_FIELD_NUMBER -> input.readMessage { inlineFileEntryName = input.readFileEntryName(fileReader) }
            ProtoFile.FILE_ENTRY_ID_FIELD_NUMBER -> fileEntryId = input.readInt32()
            // Packed (as written), or one element per tag (as a parser must also accept).
            ProtoFile.FQ_NAME_FIELD_NUMBER -> when (tag and WIRE_TYPE_MASK) {
                WireFormat.WIRETYPE_LENGTH_DELIMITED -> input.readMessage { while (!input.isAtEnd) fqName += input.readInt32() }
                else -> fqName += input.readInt32()
            }
            else -> input.skipField(tag)
        }
    }
    // Like [fileEntry]: the file entry is in the file entries table if its index is given.
    val filePath = fileEntryId?.let { id ->
        val fileEntry = irFileEntry(id, fileIndex) ?: error("Invalid KLib: cannot read file entry by its index")
        fileEntry.codedInputStream.readFileEntryName(fileReader)
    } ?: inlineFileEntryName ?: error("Invalid KLib: either fileEntry or fileEntryId must be present")
    return FileWithFqName(filePath, fileReader.deserializeFqName(fqName))
}

/** Like [deserializeFileEntryName], without parsing the line offsets of the [ProtoFileEntry]. */
private fun CodedInputStream.readFileEntryName(fileReader: IrLibraryFile): String {
    var nameIndex: Int? = null
    var nameOld: String? = null
    while (true) {
        val tag = readTag()
        if (tag == 0) break
        when (WireFormat.getTagFieldNumber(tag)) {
            ProtoFileEntry.NAME_FIELD_NUMBER -> nameIndex = readInt32()
            ProtoFileEntry.NAME_OLD_FIELD_NUMBER -> nameOld = readString()
            else -> skipField(tag)
        }
    }
    return nameIndex?.let(fileReader::string) ?: nameOld ?: error("Malformed KLIB: File entry has no name")
}

// The low bits of a tag, see `WireFormat.getTagWireType` (not public).
private const val WIRE_TYPE_MASK = 7

private inline fun CodedInputStream.readMessage(readContents: () -> Unit) {
    val oldLimit = pushLimit(readRawVarint32())
    readContents()
    popLimit(oldLimit)
}

class CacheSupport(
        private val configuration: CompilerConfiguration,
        internal val klibDag: KlibDAG,
        ignoreCacheReason: String?,
        systemCacheDirectory: Path,
        autoCacheDirectory: Path,
        incrementalCacheDirectory: Path?,
        target: KonanTarget,
        val produce: CompilerOutputKind
) {
    // Note: The order of libraries is not important here.
    private val pathToLibrary = klibDag.librariesReverseTopoSorted.associateBy { it.canonicalPath }

    private fun findLibrary(path: Path): KotlinLibrary? =
            runCatching { path.toRealPath() }.getOrNull()?.let { pathToLibrary[it] }

    private val autoCacheableFrom = configuration[NativeConfigurationKeys.AUTO_CACHEABLE_FROM]!!
            .map {
                Path(it).takeIf { it.isDirectory() }
                        ?: configuration.reportCompilationErrorAndThrow("auto cacheable root $it is not found or is not a directory")
            }

    private val implicitCacheDirectories = buildList {
        configuration[NativeConfigurationKeys.CACHE_DIRECTORIES]!!.forEach {
            add(Path(it).takeIf { it.isDirectory() }
                    ?: configuration.reportCompilationErrorAndThrow("cache directory $it is not found or is not a directory"))
        }
        systemCacheDirectory.takeIf { autoCacheableFrom.isNotEmpty() || incrementalCacheDirectory != null }?.let { add(it) }
        autoCacheDirectory.takeIf { autoCacheableFrom.isNotEmpty() }?.let { add(it) }
        incrementalCacheDirectory?.let { add(it) }
    }

    internal fun tryGetImplicitOutput(cacheInfo: PartialCacheInfo?): String? {
        val libraryToCache = cacheInfo ?: return null
        // Put the resulting library in the first cache directory.
        val cacheDirectory = implicitCacheDirectories.firstOrNull() ?: return null
        val singleFileStrategy = libraryToCache.strategy as? CacheDeserializationStrategy.SingleFile
        val baseLibraryCacheDirectory = cacheDirectory.resolve(
                if (singleFileStrategy == null)
                    CachedLibraries.getCachedLibraryName(libraryToCache.klib)
                else
                    CachedLibraries.getPerFileCachedLibraryName(libraryToCache.klib)
        )
        val singleFilePath = singleFileStrategy?.filePath
                ?: return baseLibraryCacheDirectory.absolutePathString()

        val fileCacheDirectory = baseLibraryCacheDirectory.resolve(cacheFileId(singleFileStrategy.fqName, singleFilePath))
        return fileCacheDirectory.absolutePathString()
    }

    /**
     * The files whose per-file caches are rebuilt by this compilation, for the libraries being cached partially:
     * either the single library to cache, or the libraries of a cache batch having their dirty files rebuilt.
     */
    internal val filesBeingCached: Map<KotlinLibrary, List<String>> =
            configuration[CACHE_BATCH_FILES]?.entries?.associate { getLibrary(Path(it.key)) to it.value }
                    ?: configuration.konanLibraryToAddToCache?.let { mapOf(getLibrary(Path(it)) to configuration.filesToCache) }
                    ?: emptyMap()

    internal val cachedLibraries: CachedLibraries = run {
        val explicitCacheFiles = configuration[NativeConfigurationKeys.CACHED_LIBRARIES]!!

        val explicitCaches = explicitCacheFiles.entries.associate { [libraryPath, cachePath] ->
            val library = findLibrary(Path(libraryPath))
                    ?: configuration.reportCompilationErrorAndThrow("cache not applied: library $libraryPath in $cachePath")

            library to cachePath
        }

        val hasCachedLibs = explicitCacheFiles.isNotEmpty() || implicitCacheDirectories.isNotEmpty()

        if (ignoreCacheReason != null && hasCachedLibs) {
            configuration.report(CliDiagnostics.KONAN_ARGUMENT_WARNING, "Cached libraries will not be used $ignoreCacheReason")
        }

        val ignoreCachedLibraries = ignoreCacheReason != null
        CachedLibraries(
                configuration = configuration,
                target = target,
                klibDag = klibDag,
                explicitCaches = if (ignoreCachedLibraries) emptyMap() else explicitCaches,
                implicitCacheDirectories = if (ignoreCachedLibraries) emptyList() else implicitCacheDirectories,
                autoCacheDirectory = autoCacheDirectory,
                autoCacheableFrom = if (ignoreCachedLibraries) emptyList() else autoCacheableFrom,
                filesBeingCached = filesBeingCached,
        )
    }

    private fun getLibrary(path: Path) =
            findLibrary(path) ?: error("library to cache\n" +
                    "  ${path.pathString}\n" +
                    "not found among resolved libraries:\n  " +
                    klibDag.librariesReverseTopoSorted.joinToString("\n  ") { it.path.pathString })

    internal val librariesToCache: Map<KotlinLibrary, PartialCacheInfo> = configuration[CACHE_BATCH]?.let { batch ->
        require(configuration.konanLibraryToAddToCache == null) { "Batched build cannot be mixed with single cache build" }
        require(configuration.makePerFileCache) {
            "Batched build is only supported for per-file caches as a part of incremental build"
        }
        require(batch.keys.containsAll(configuration[CACHE_BATCH_FILES]?.keys.orEmpty())) {
            "Files to cache are specified for libraries out of the cache batch"
        }
        batch.keys.associate { path ->
            val library = getLibrary(Path(path))
            val cache = cachedLibraries.getLibraryCache(library, allowIncomplete = true)
            val filesToCache = filesBeingCached[library].orEmpty()
            val strategy = if (filesToCache.isEmpty()) {
                require(cache == null) { "Cache batch contains an already cached library: ${library.path}" }
                CacheDeserializationStrategy.WholeModule
            } else {
                // Like for the single library to cache, there might be no cache found at all, e.g. when all the files
                // the cache of the library had got removed (or renamed), leaving its per-file cache directory empty.
                require(cache !is CachedLibraries.Cache.Monolithic) {
                    "Cache batch rebuilds some files of a library having a monolithic cache: ${library.path}"
                }
                CacheDeserializationStrategy.MultipleFiles(filesToCache, library.getFileFqNames(filesToCache))
            }
            library to PartialCacheInfo(library, strategy)
        }
    } ?: configuration.konanLibraryToAddToCache?.let {
        val libraryToAddToCacheFile = Path(it)
        val libraryToAddToCache = getLibrary(libraryToAddToCacheFile)
        val libraryCache = cachedLibraries.getLibraryCache(libraryToAddToCache, allowIncomplete = true)
        if (libraryCache is CachedLibraries.Cache.Monolithic)
            null
        else {
            val filesToCache = configuration.filesToCache

            val strategy = if (filesToCache.isEmpty())
                CacheDeserializationStrategy.WholeModule
            else
                CacheDeserializationStrategy.MultipleFiles(filesToCache, libraryToAddToCache.getFileFqNames(filesToCache))
            mapOf(libraryToAddToCache to PartialCacheInfo(libraryToAddToCache, strategy))
        }
    } ?: emptyMap()

    internal val preLinkCaches: Boolean =
            configuration.preLinkCaches

    companion object {
        fun cacheFileId(fqName: String, filePath: String) =
                "${if (fqName == "") "ROOT" else fqName}.${filePath.hashCode().toString(Character.MAX_RADIX)}"
    }

    fun checkConsistency() {
        // Note: The libraries should be in the reverse topo-order here.
        for (library in klibDag.librariesReverseTopoSorted) {
            val cache = cachedLibraries.getLibraryCache(library)
            if (cache != null || library in librariesToCache) {
                val dependencies = klibDag.getAllDependencies(library)
                for (dependency in dependencies) {
                    if (!cachedLibraries.isLibraryCached(dependency) && dependency !in librariesToCache) {
                        val description = if (cache != null) "cached (in ${cache.path})" else "going to be cached"
                        configuration.reportCompilationErrorAndThrow("${library.path} is $description, but its dependency isn't: ${dependency.path}")
                    }
                }
            }
        }

        // Ensure not making cache for libraries that are already cached:
        librariesToCache.keys.forEach { library ->
            val cache = cachedLibraries.getLibraryCache(library)
            if (cache is CachedLibraries.Cache.Monolithic) {
                configuration.reportCompilationErrorAndThrow("can't cache library '${library.path}' " +
                        "that is already cached in '${cache.path}'")
            }
        }
    }
}

internal class FileIdProvider(private val deserializer: KonanPartialModuleDeserializer) {
    val sortedFileIds by lazy {
        deserializer.getDeserializationStates()
                .sortedBy { it.file.fileEntry.name }
                .map { CacheSupport.cacheFileId(it.file.packageFqName.asString(), it.file.fileEntry.name) }
    }
}
