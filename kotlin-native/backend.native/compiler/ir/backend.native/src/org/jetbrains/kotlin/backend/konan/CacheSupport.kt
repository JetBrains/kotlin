/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult

class CacheSupport(
        val configuration: CompilerConfiguration,
        resolvedLibraries: KotlinLibraryResolveResult,
        target: KonanTarget,
        produce: CompilerOutputKind
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
        val libraryToAddToCache = configuration.get(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE) ?: return null
        // Put the resulting library in the first cache directory.
        val cacheDirectory = implicitCacheDirectories.firstOrNull() ?: return null
        val libraryToAddToCacheFile = File(libraryToAddToCache)
        val library = allLibraries.single { it.libraryFile == libraryToAddToCacheFile }
        return cacheDirectory.child(CachedLibraries.getCachedLibraryName(library)).absolutePath
    }


    internal val cachedLibraries: CachedLibraries = run {
        val explicitCacheFiles = configuration.get(KonanConfigKeys.CACHED_LIBRARIES)!!

        val explicitCaches = explicitCacheFiles.entries.associate { (libraryPath, cachePath) ->
            val library = fileToLibrary[File(libraryPath)]
                    ?: configuration.reportCompilationError("cache not applied: library $libraryPath in $cachePath")

            library to cachePath
        }

        val hasCachedLibs = explicitCacheFiles.isNotEmpty() || implicitCacheDirectories.isNotEmpty()

        val ignoreReason = when {
            configuration.getBoolean(KonanConfigKeys.OPTIMIZATION) -> "for optimized compilation"
            configuration.get(BinaryOptions.memoryModel) == MemoryModel.EXPERIMENTAL -> "with experimental memory model"
            configuration.getBoolean(KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION) -> "with experimental lazy top levels initialization"
            configuration.get(BinaryOptions.stripDebugInfoFromNativeLibs) == false -> "with native libs debug info"
            else -> null
        }

        if (ignoreReason != null && hasCachedLibs) {
            configuration.report(CompilerMessageSeverity.WARNING, "Cached libraries will not be used $ignoreReason")
        }

        val ignoreCachedLibraries = ignoreReason != null
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

    internal val librariesToCache: Set<KotlinLibrary> = run {
        val libraryToAddToCachePath = configuration.get(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE)
        if (libraryToAddToCachePath.isNullOrEmpty()) {
            configuration.get(KonanConfigKeys.LIBRARIES_TO_CACHE)!!
                    .map { getLibrary(File(it)) }
                    .toSet()
                    .also { if (!produce.isCache) check(it.isEmpty()) }
        } else {
            val libraryToAddToCacheFile = File(libraryToAddToCachePath)
            val libraryToAddToCache = getLibrary(libraryToAddToCacheFile)
            val libraryCache = cachedLibraries.getLibraryCache(libraryToAddToCache)
            if (libraryCache == null)
                setOf(libraryToAddToCache)
            else
                emptySet()
        }
    }

    internal val preLinkCaches: Boolean =
            configuration.get(KonanConfigKeys.PRE_LINK_CACHES, false)

    init {
        // Ensure dependencies of every cached library are cached too:
        resolvedLibraries.getFullList { libraries ->
            libraries.map { library ->
                val cache = cachedLibraries.getLibraryCache(library.library)
                if (cache != null || library.library in librariesToCache) {
                    library.resolvedDependencies.forEach {
                        if (!cachedLibraries.isLibraryCached(it.library) && it.library !in librariesToCache) {
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
        librariesToCache.forEach {
            val cache = cachedLibraries.getLibraryCache(it)
            if (cache != null) {
                configuration.reportCompilationError("Can't cache library '${it.libraryName}' " +
                        "that is already cached in '${cache.path}'")
            }
        }

        if ((librariesToCache.isNotEmpty() || cachedLibraries.hasDynamicCaches || cachedLibraries.hasStaticCaches)
                && configuration.getBoolean(KonanConfigKeys.OPTIMIZATION)) {
            configuration.reportCompilationError("Cache cannot be used in optimized compilation")
        }
    }
}