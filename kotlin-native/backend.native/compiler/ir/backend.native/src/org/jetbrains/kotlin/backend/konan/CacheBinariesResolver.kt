/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * Check if we should link static caches into an object file before running full linkage.
 */
internal fun shouldPerformPreLink(config: NativeSecondStageCompilationConfig, caches: ResolvedCacheBinaries, linkerOutputKind: LinkerOutputKind): Boolean {
    // Pre-link is only useful when producing static library. Otherwise its just a waste of time.
    val isStaticLibrary = linkerOutputKind == LinkerOutputKind.STATIC_LIBRARY &&
            config.isFinalBinary
    val enabled = config.cacheSupport.preLinkCaches
    val nonEmptyCaches = caches.staticLibraries.isNotEmpty()
    return isStaticLibrary && enabled && nonEmptyCaches
}

internal data class ResolvedLibraryCacheBinaries(
        val library: KotlinLibrary,
        val binaries: List<String>,
)

/**
 * List of cache binaries that are required for the final artifact.
 * - [staticBinaryPaths] is a list of static library paths (e.g. "libcache.a")
 * - [dynamicBinaryPaths] is a list of dynamic library paths (e.g. "libcache.dylib")
 */
internal data class ResolvedCacheBinaries(
        val staticLibraries: List<ResolvedLibraryCacheBinaries>,
        val dynamicLibraries: List<ResolvedLibraryCacheBinaries>,

) {
    val isEmpty: Boolean
        get() = staticBinaryPaths.isEmpty() && dynamicBinaryPaths.isEmpty()

    val staticBinaryPaths: List<String> by lazy {
        staticLibraries.flatMap { it.binaries }
    }

    val dynamicBinaryPaths: List<String> by lazy {
        dynamicLibraries.flatMap { it.binaries }
    }
}

/**
 * Find binary files for compiler caches that are actually required for the linkage.
 */
internal fun resolveCacheBinaries(
        cachedLibraries: CachedLibraries,
        dependenciesTrackingResult: DependenciesTrackingResult,
): ResolvedCacheBinaries {

    val staticLibraries = mutableListOf<ResolvedLibraryCacheBinaries>()
    val dynamicLibraries = mutableListOf<ResolvedLibraryCacheBinaries>()

    dependenciesTrackingResult.allCachedBitcodeDependencies.forEach { dependency ->
        val library = dependency.library
        val cache = cachedLibraries.getLibraryCache(library)
        // Maybe turn it into a warning and continue linkage without caches?
                ?: error("Library $library is expected to be cached")

        val list = when (cache.kind) {
            CachedLibraries.Kind.DYNAMIC -> dynamicLibraries
            CachedLibraries.Kind.STATIC -> staticLibraries
            CachedLibraries.Kind.HEADER -> error("Header cache ${cache.path} cannot be used for linking")
        }

        val binaries = if (dependency.kind is DependenciesTracker.DependencyKind.CertainFiles && cache is CachedLibraries.Cache.PerFile)
            dependency.kind.files.map { cache.getFileBinaryPath(it.name) }
        else cache.binariesPaths

        list += ResolvedLibraryCacheBinaries(library, binaries)
    }

    return ResolvedCacheBinaries(staticLibraries, dynamicLibraries)
}
