/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.phases.PreLinkCachesPhase
import org.jetbrains.kotlin.konan.target.LinkerOutputKind

/**
 * When pre-link caches flag is enabled, check if we should invoke the [PreLinkCachesPhase],
 * Per-linking is performed only when: the linker output kind is a static library (i.e., an archive file `.a`),
 * the produced artifact is the final binary and the caches are non-empty.
 */
internal fun shouldPerformPreLink(config: NativeSecondStageCompilationConfig, caches: ResolvedCacheBinaries, linkerOutputKind: LinkerOutputKind): Boolean {
    // Pre-link is only useful when producing static library. Otherwise its just a waste of time.
    val isStaticLibrary = linkerOutputKind == LinkerOutputKind.STATIC_LIBRARY && config.isFinalBinary
    val enabled = config.cacheSupport.preLinkCaches
    val nonEmptyCaches = caches.staticLibraryPaths.isNotEmpty() || caches.objectFilePaths.isNotEmpty()
    return isStaticLibrary && enabled && nonEmptyCaches
}

/**
 * List of cache binaries that are required for the final artifact.
 * - [staticLibraryPaths] is a list of static libraries (e.g. "libcache.a")
 * - [dynamicLibraryPaths] is a list of dynamic libraries (e.g. "libcache.dylib")
 * - [objectFilePaths] is a list of object files from per-file caches (e.g. "<fileId>.o")
 */
internal class ResolvedCacheBinaries(
        val staticLibraryPaths: List<String>,
        val dynamicLibraryPaths: List<String>,
        val objectFilePaths: List<String>
) {
    companion object {
        val EMPTY: ResolvedCacheBinaries = ResolvedCacheBinaries(
                emptyList(),
                emptyList(),
                emptyList()
        )
    }

    fun isEmpty(): Boolean =
            staticLibraryPaths.isEmpty() && dynamicLibraryPaths.isEmpty() && objectFilePaths.isEmpty()
}

/**
 * Find binary files for compiler caches that are actually required for the linkage.
 */
internal fun resolveCacheBinaries(
        cachedLibraries: CachedLibraries,
        dependenciesTrackingResult: DependenciesTrackingResult,
): ResolvedCacheBinaries {
    val staticLibraryCaches = mutableListOf<String>()
    val dynamicLibraryCaches = mutableListOf<String>()
    val objectFileCaches = mutableListOf<String>()

    dependenciesTrackingResult.allCachedBitcodeDependencies.forEach { (val library, val kind) ->
        // Maybe turn it into a warning and continue linkage without caches?
        val cache = cachedLibraries.getLibraryCache(library) ?: error("Library $library is expected to be cached")
        // Per-file caches store plain object files, see [CachedLibraries.getPerFileCacheObjectFileName].
        val targetList = if (cache is CachedLibraries.Cache.PerFile) objectFileCaches else when (cache.kind) {
            CachedLibraries.Kind.DYNAMIC -> dynamicLibraryCaches
            CachedLibraries.Kind.STATIC -> staticLibraryCaches
            CachedLibraries.Kind.HEADER -> error("Header cache ${cache.path} cannot be used for linking")
        }
        targetList += when {
            kind is DependenciesTracker.DependencyKind.CertainFiles && cache is CachedLibraries.Cache.PerFile ->
                kind.files.map { cache.getFileBinaryPath(it.name) }
            else -> cache.binariesPaths
        }
    }

    return ResolvedCacheBinaries(
            staticLibraryCaches,
            dynamicLibraryCaches,
            objectFileCaches
    )
}
