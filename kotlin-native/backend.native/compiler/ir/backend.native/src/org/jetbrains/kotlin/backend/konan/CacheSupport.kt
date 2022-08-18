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

    class SingleFile(val filePath: String) : CacheDeserializationStrategy() {
        override fun contains(filePath: String) = filePath == this.filePath

        lateinit var fqName: String

        override fun contains(fqName: FqName, fileName: String) =
                fqName.asString() == this.fqName && File(filePath).name == fileName
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

    internal fun tryGetImplicitOutput(): String? {
        val libraryToCache = libraryToCache ?: return null
        // Put the resulting library in the first cache directory.
        val cacheDirectory = implicitCacheDirectories.firstOrNull() ?: return null
        val singleFileStrategy = libraryToCache.strategy as? CacheDeserializationStrategy.SingleFile
        val baseLibraryCacheDirectory = cacheDirectory.child(
                if (singleFileStrategy == null)
                    CachedLibraries.getCachedLibraryName(libraryToCache.klib)
                else
                    CachedLibraries.getPerFileCachedLibraryName(libraryToCache.klib)
        )
        if (singleFileStrategy == null)
            return baseLibraryCacheDirectory.absolutePath

        val fileToCache = singleFileStrategy.filePath
        val fileProtos = Array<ProtoFile>(libraryToCache.klib.fileCount()) {
            ProtoFile.parseFrom(libraryToCache.klib.file(it).codedInputStream, ExtensionRegistryLite.newInstance())
        }
        val fileIndex = fileProtos.indexOfFirst { it.fileEntry.name == fileToCache }
        require(fileIndex >= 0) { "No file found in klib with path $fileToCache" }
        val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(libraryToCache.klib, fileIndex))
        val fqName = fileReader.deserializeFqName(fileProtos[fileIndex].fqNameList)
        singleFileStrategy.fqName = fqName
        val fileCacheDirectory = baseLibraryCacheDirectory.child(cacheFileId(fqName, fileToCache))
        val contentDirName = if (produce == CompilerOutputKind.PRELIMINARY_CACHE)
            CachedLibraries.PER_FILE_CACHE_IR_LEVEL_DIR_NAME
        else CachedLibraries.PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME
        return fileCacheDirectory.child(contentDirName).absolutePath
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
            val fileToCache = configuration.get(KonanConfigKeys.FILE_TO_CACHE)
            PartialCacheInfo(libraryToAddToCache, if (fileToCache == null) CacheDeserializationStrategy.WholeModule else CacheDeserializationStrategy.SingleFile(fileToCache))
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
