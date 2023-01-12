/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.backend.konan.descriptors.isInteropLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies

internal fun KotlinLibrary.getAllTransitiveDependencies(allLibraries: Map<String, KotlinLibrary>): List<KotlinLibrary> {
    val allDependencies = mutableSetOf<KotlinLibrary>()

    fun traverseDependencies(library: KotlinLibrary) {
        library.unresolvedDependencies.forEach {
            val dependency = allLibraries[it.path]!!
            if (dependency !in allDependencies) {
                allDependencies += dependency
                traverseDependencies(dependency)
            }
        }
    }

    traverseDependencies(this)
    return allDependencies.toList()
}

class CacheBuilder(
        val konanConfig: KonanConfig,
        val spawnCompilation: (List<String>, CompilerConfiguration.() -> Unit) -> Unit
) {
    private val configuration = konanConfig.configuration
    private val autoCacheableFrom = configuration.get(KonanConfigKeys.AUTO_CACHEABLE_FROM)!!.map { File(it) }

    fun needToBuild() = konanConfig.isFinalBinary && konanConfig.ignoreCacheReason == null && autoCacheableFrom.isNotEmpty()

    fun build() {
        val allLibraries = konanConfig.resolvedLibraries.getFullList(TopologicalLibraryOrder)
        val uniqueNameToLibrary = allLibraries.associateBy { it.uniqueName }
        val caches = mutableMapOf<KotlinLibrary, String>()

        allLibraries.forEach librariesLoop@{ library ->
            konanConfig.cachedLibraries.getLibraryCache(library)?.let {
                caches[library] = it.rootDirectory
                return@librariesLoop
            }
            val libraryPath = library.libraryFile.absolutePath
            val isLibraryAutoCacheable = library.isDefault || autoCacheableFrom.any { libraryPath.startsWith(it.absolutePath) }
            if (!isLibraryAutoCacheable)
                return@librariesLoop

            val dependencies = library.getAllTransitiveDependencies(uniqueNameToLibrary)
            val dependencyCaches = dependencies.map {
                caches[it] ?: run {
                    configuration.report(CompilerMessageSeverity.LOGGING,
                            "SKIPPING ${library.libraryName} as some of the dependencies aren't cached")
                    return@librariesLoop
                }
            }

            // TODO: Uncomment after making per-file caches default.
            val makePerFileCache = false//!library.isInteropLibrary()
            configuration.report(CompilerMessageSeverity.LOGGING, "CACHING ${library.libraryName}")
            val libraryCacheDirectory = if (library.isDefault)
                konanConfig.systemCacheDirectory
            else
                CachedLibraries.computeVersionedCacheDirectory(konanConfig.autoCacheDirectory, library, uniqueNameToLibrary)
            val libraryCache = libraryCacheDirectory.child(
                    if (makePerFileCache)
                        CachedLibraries.getPerFileCachedLibraryName(library)
                    else
                        CachedLibraries.getCachedLibraryName(library)
            )
            try {
                // TODO: Run monolithic cache builds in parallel.
                libraryCacheDirectory.mkdirs()
                spawnCompilation(konanConfig.additionalCacheFlags /* TODO: Some way to put them directly to CompilerConfiguration? */) {
                    val libraries = dependencies.filter { !it.isDefault }.map { it.libraryFile.absolutePath }
                    val cachedLibraries = dependencies.zip(dependencyCaches).associate { it.first.libraryFile.absolutePath to it.second }
                    configuration.report(CompilerMessageSeverity.LOGGING, "    dependencies:\n        " +
                            libraries.joinToString("\n        "))
                    configuration.report(CompilerMessageSeverity.LOGGING, "    caches used:\n        " +
                            cachedLibraries.entries.joinToString("\n        ") { "${it.key}: ${it.value}" })
                    configuration.report(CompilerMessageSeverity.LOGGING, "    cache dir: " +
                            libraryCacheDirectory.absolutePath)

                    setupCommonOptionsForCaches(konanConfig)
                    put(KonanConfigKeys.PRODUCE, CompilerOutputKind.STATIC_CACHE)
                    put(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE, libraryPath)
                    put(KonanConfigKeys.NODEFAULTLIBS, true)
                    put(KonanConfigKeys.NOENDORSEDLIBS, true)
                    put(KonanConfigKeys.NOSTDLIB, true)
                    put(KonanConfigKeys.LIBRARY_FILES, libraries)
                    put(KonanConfigKeys.CACHED_LIBRARIES, cachedLibraries)
                    put(KonanConfigKeys.CACHE_DIRECTORIES, listOf(libraryCacheDirectory.absolutePath))
                    put(KonanConfigKeys.MAKE_PER_FILE_CACHE, makePerFileCache)
                }
                caches[library] = libraryCache.absolutePath
            } catch (t: Throwable) {
                configuration.report(CompilerMessageSeverity.LOGGING, "${t.message}\n${t.stackTraceToString()}")
                configuration.report(CompilerMessageSeverity.WARNING,
                        "Failed to build cache: ${t.message}\n${t.stackTraceToString()}\n" +
                                "Falling back to not use cache for ${library.libraryName}")

                libraryCache.deleteRecursively()
            }
        }
    }
}