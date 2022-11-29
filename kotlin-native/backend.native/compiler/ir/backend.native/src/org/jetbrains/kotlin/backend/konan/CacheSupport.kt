/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.IrKlibBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.deserializeFqName
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

sealed class CacheDeserializationStrategy {
    abstract fun contains(filePath: String): Boolean
    abstract fun contains(fqName: FqName, fileName: String): Boolean

    object Nothing : CacheDeserializationStrategy() {
        override fun contains(filePath: String) = false
        override fun contains(fqName: FqName, fileName: String) = false
    }

    object WholeModule : CacheDeserializationStrategy() {
        override fun contains(filePath: String) = true
        override fun contains(fqName: FqName, fileName: String) = true
    }

    class SingleFile(val filePath: String, val fqName: String) : CacheDeserializationStrategy() {
        override fun contains(filePath: String) = filePath == this.filePath

        override fun contains(fqName: FqName, fileName: String) =
                fqName.asString() == this.fqName && File(filePath).name == fileName
    }

    class MultipleFiles(filePaths: List<String>, fqNames: List<String>) : CacheDeserializationStrategy() {
        private val filePaths = filePaths.toSet()

        private val fqNamesWithNames = fqNames.mapIndexed { i: Int, fqName: String -> Pair(fqName, File(filePaths[i]).name) }.toSet()

        override fun contains(filePath: String) = filePath in filePaths

        override fun contains(fqName: FqName, fileName: String) = Pair(fqName.asString(), fileName) in fqNamesWithNames
    }
}

class PartialCacheInfo(val klib: KotlinLibrary, val strategy: CacheDeserializationStrategy)

class CacheSupport(
        private val configuration: CompilerConfiguration,
        resolvedLibraries: KotlinLibraryResolveResult,
        ignoreCacheReason: String?,
        target: KonanTarget,
        val produce: CompilerOutputKind
) {
    private val allLibraries = resolvedLibraries.getFullList()

    // TODO: consider using [FeaturedLibraries.kt].
    private val fileToLibrary = allLibraries.associateBy { it.libraryFile }

    private val implicitCacheDirectories = configuration.get(KonanConfigKeys.CACHE_DIRECTORIES)!!
            .map {
                File(it).takeIf { it.isDirectory }
                        ?: configuration.reportCompilationError("cache directory $it is not found or not a directory")
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
                implicitCacheDirectories = if (ignoreCachedLibraries) emptyList() else implicitCacheDirectories
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
        if (libraryCache != null && libraryCache.granularity == CachedLibraries.Granularity.MODULE)
            null
        else {
            val filesToCache = configuration.get(KonanConfigKeys.FILES_TO_CACHE)

            val strategy = if (filesToCache.isNullOrEmpty())
                CacheDeserializationStrategy.WholeModule
            else {
                val fileProtos = Array<ProtoFile>(libraryToAddToCache.fileCount()) {
                    ProtoFile.parseFrom(libraryToAddToCache.file(it).codedInputStream, ExtensionRegistryLite.newInstance())
                }
                val fileNameToIndex = fileProtos.withIndex().associate { it.value.fileEntry.name to it.index }
                val fqNames = filesToCache.map { fileName ->
                    val index = fileNameToIndex[fileName] ?: error("No file with path $fileName is found in klib ${libraryToAddToCache.libraryName}")
                    val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(libraryToAddToCache, index))
                    fileReader.deserializeFqName(fileProtos[index].fqNameList)
                }
                CacheDeserializationStrategy.MultipleFiles(filesToCache, fqNames)
            }
            PartialCacheInfo(libraryToAddToCache, strategy)
        }
    }

    internal val preLinkCaches: Boolean =
            configuration.get(KonanConfigKeys.PRE_LINK_CACHES, false)

    companion object {
        fun cacheFileId(fqName: String, filePath: String) = "${if (fqName == "") "ROOT" else fqName}.${filePath.hashCode().toString(Character.MAX_RADIX)}"
    }

    init {
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
            if (cache != null && cache.granularity == CachedLibraries.Granularity.MODULE) {
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
