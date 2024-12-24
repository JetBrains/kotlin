/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FileDeserializationState
import org.jetbrains.kotlin.backend.common.serialization.IrKlibBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.deserializeFqName
import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.backend.konan.serialization.KonanPartialModuleDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.PartialCacheInfo
import org.jetbrains.kotlin.backend.common.serialization.fileEntry
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

class FileWithFqName(val filePath: String, val fqName: String)

fun KotlinLibrary.getFilesWithFqNames(): List<FileWithFqName> {
    val fileProtos = Array<ProtoFile>(fileCount()) {
        ProtoFile.parseFrom(file(it).codedInputStream, ExtensionRegistryLite.newInstance())
    }
    return fileProtos.mapIndexed { index, proto ->
        val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(this, index))
        FileWithFqName(fileReader.fileEntry(proto).name, fileReader.deserializeFqName(proto.fqNameList))
    }
}

fun KotlinLibrary.getFileFqNames(filePaths: List<String>): List<String> {
    val fileProtos = Array<ProtoFile>(fileCount()) {
        ProtoFile.parseFrom(file(it).codedInputStream, ExtensionRegistryLite.newInstance())
    }
    val filePathToIndex = fileProtos.withIndex().associate {
        fileEntry(it.value, it.index).name to it.index
    }
    return filePaths.map { filePath ->
        val index = filePathToIndex[filePath] ?: error("No file with path $filePath is found in klib $libraryName")
        val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(this, index))
        fileReader.deserializeFqName(fileProtos[index].fqNameList)
    }
}

class CacheSupport(
        private val configuration: CompilerConfiguration,
        private val resolvedLibraries: KotlinLibraryResolveResult,
        ignoreCacheReason: String?,
        systemCacheDirectory: File,
        autoCacheDirectory: File,
        incrementalCacheDirectory: File?,
        target: KonanTarget,
        val produce: CompilerOutputKind
) {
    private val allLibraries = resolvedLibraries.getFullList()

    // TODO: consider using [FeaturedLibraries.kt].
    private val fileToLibrary = allLibraries.associateBy { it.libraryFile }

    private val autoCacheableFrom = configuration.get(KonanConfigKeys.AUTO_CACHEABLE_FROM)!!
            .map {
                File(it).takeIf { it.isDirectory }
                        ?: configuration.reportCompilationError("auto cacheable root $it is not found or is not a directory")
            }

    private val implicitCacheDirectories = buildList {
        configuration.get(KonanConfigKeys.CACHE_DIRECTORIES)!!.forEach {
            add(File(it).takeIf { it.isDirectory }
                    ?: configuration.reportCompilationError("cache directory $it is not found or is not a directory"))
        }
        systemCacheDirectory.takeIf { autoCacheableFrom.isNotEmpty() || incrementalCacheDirectory != null }?.let { add(it) }
        autoCacheDirectory.takeIf { autoCacheableFrom.isNotEmpty() }?.let { add(it) }
        incrementalCacheDirectory?.let { add(it) }
    }

    internal fun tryGetImplicitOutput(cacheDeserializationStrategy: CacheDeserializationStrategy?): String? {
        val libraryToCache = libraryToCache ?: return null
        // Put the resulting library in the first cache directory.
        val cacheDirectory = implicitCacheDirectories.firstOrNull() ?: return null
        val singleFileStrategy = cacheDeserializationStrategy as? CacheDeserializationStrategy.SingleFile
        val baseLibraryCacheDirectory = cacheDirectory.child(
                if (singleFileStrategy == null)
                    CachedLibraries.getCachedLibraryName(libraryToCache.klib)
                else
                    CachedLibraries.getPerFileCachedLibraryName(libraryToCache.klib)
        )
        val singleFilePath = singleFileStrategy?.filePath
                ?: return baseLibraryCacheDirectory.absolutePath

        val fileCacheDirectory = baseLibraryCacheDirectory.child(cacheFileId(singleFileStrategy.fqName, singleFilePath))
        return fileCacheDirectory.absolutePath
    }

    internal val cachedLibraries: CachedLibraries = run {
        val explicitCacheFiles = configuration.get(KonanConfigKeys.CACHED_LIBRARIES)!!

        val explicitCaches = explicitCacheFiles.entries.associate { (libraryPath, cachePath) ->
            val library = fileToLibrary[File(libraryPath)]
                    ?: configuration.reportCompilationError("cache not applied: library $libraryPath in $cachePath")

            library to cachePath
        }

        val hasCachedLibs = explicitCacheFiles.isNotEmpty() || implicitCacheDirectories.isNotEmpty()

        if (ignoreCacheReason != null && hasCachedLibs) {
            configuration.report(CompilerMessageSeverity.WARNING, "Cached libraries will not be used $ignoreCacheReason")
        }

        val ignoreCachedLibraries = ignoreCacheReason != null
        CachedLibraries(
                target = target,
                allLibraries = allLibraries,
                explicitCaches = if (ignoreCachedLibraries) emptyMap() else explicitCaches,
                implicitCacheDirectories = if (ignoreCachedLibraries) emptyList() else implicitCacheDirectories,
                autoCacheDirectory = autoCacheDirectory,
                autoCacheableFrom = if (ignoreCachedLibraries) emptyList() else autoCacheableFrom
        )
    }

    private fun getLibrary(file: File) =
            fileToLibrary[file] ?: error("library to cache\n" +
                    "  ${file.absolutePath}\n" +
                    "not found among resolved libraries:\n  " +
                    allLibraries.joinToString("\n  ") { it.libraryFile.absolutePath })

    internal val libraryToCache = configuration.get(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE)?.let {
        val libraryToAddToCacheFile = File(it)
        val libraryToAddToCache = getLibrary(libraryToAddToCacheFile)
        val libraryCache = cachedLibraries.getLibraryCache(libraryToAddToCache)
        if (libraryCache is CachedLibraries.Cache.Monolithic)
            null
        else {
            val filesToCache = configuration.get(KonanConfigKeys.FILES_TO_CACHE)

            val strategy = if (filesToCache.isNullOrEmpty())
                CacheDeserializationStrategy.WholeModule
            else
                CacheDeserializationStrategy.MultipleFiles(filesToCache, libraryToAddToCache.getFileFqNames(filesToCache))
            PartialCacheInfo(libraryToAddToCache, strategy)
        }
    }

    internal val preLinkCaches: Boolean =
            configuration.get(KonanConfigKeys.PRE_LINK_CACHES, false)

    companion object {
        fun cacheFileId(fqName: String, filePath: String) =
                "${if (fqName == "") "ROOT" else fqName}.${filePath.hashCode().toString(Character.MAX_RADIX)}"
    }

    fun checkConsistency() {
        // Ensure dependencies of every cached library are cached too:
        resolvedLibraries.getFullList { libraries ->
            libraries.map { library ->
                val cache = cachedLibraries.getLibraryCache(library.library)
                if (cache != null || library.library == libraryToCache?.klib) {
                    library.resolvedDependencies.forEach {
                        if (!cachedLibraries.isLibraryCached(it.library) && it.library != libraryToCache?.klib) {
                            val description = if (cache != null) {
                                "cached (in ${cache.path})"
                            } else {
                                "going to be cached"
                            }
                            configuration.reportCompilationError(
                                    "${library.library.libraryName} is $description, " +
                                            "but its dependency isn't: ${it.library.libraryName}"
                            )
                        }
                    }
                }

                library
            }
        }

        // Ensure not making cache for libraries that are already cached:
        libraryToCache?.klib?.let {
            val cache = cachedLibraries.getLibraryCache(it)
            if (cache is CachedLibraries.Cache.Monolithic) {
                configuration.reportCompilationError("can't cache library '${it.libraryName}' " +
                        "that is already cached in '${cache.path}'")
            }
        }

        if ((libraryToCache != null || cachedLibraries.hasDynamicCaches || cachedLibraries.hasStaticCaches)
                && configuration.getBoolean(KonanConfigKeys.OPTIMIZATION)) {
            configuration.reportCompilationError("Cache cannot be used in optimized compilation")
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
