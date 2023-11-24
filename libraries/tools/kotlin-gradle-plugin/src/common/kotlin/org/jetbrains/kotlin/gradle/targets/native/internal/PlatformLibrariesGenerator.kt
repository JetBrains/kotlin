/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.KotlinNativeLibraryGenerationRunner
import org.jetbrains.kotlin.compilerRunner.getKonanCacheKind
import org.jetbrains.kotlin.compilerRunner.konanDataDir
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.tasks.CacheBuilder
import org.jetbrains.kotlin.gradle.tasks.addArg
import org.jetbrains.kotlin.gradle.utils.lifecycleWithDuration
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import org.jetbrains.kotlin.konan.library.KONAN_PLATFORM_LIBS_NAME_PREFIX
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.customerDistribution
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class PlatformLibrariesGenerator(val project: Project, val konanTarget: KonanTarget) {

    private val distribution =
        customerDistribution(project.konanHome.absolutePath, konanDataDir = project.konanDataDir)

    private val platformLibsDirectory =
        File(distribution.platformLibs(konanTarget)).absoluteFile

    private val defDirectory =
        File(distribution.platformDefs(konanTarget)).absoluteFile

    private val konanPropertiesService: KonanPropertiesBuildService
        get() = KonanPropertiesBuildService.registerIfAbsent(project).get()

    private val konanCacheKind: NativeCacheKind by lazy {
        project.getKonanCacheKind(konanTarget)
    }

    private val shouldBuildCaches: Boolean =
        konanPropertiesService.cacheWorksFor(konanTarget) && konanCacheKind != NativeCacheKind.NONE

    private val presentDefs: Set<String> by lazy {
        defDirectory
            .listFiles { file -> file.extension == "def" }.orEmpty()
            .map { it.nameWithoutExtension }.toSet()
    }

    private fun Set<String>.toPlatformLibNames(): Set<String> =
        mapTo(mutableSetOf()) { "$KONAN_PLATFORM_LIBS_NAME_PREFIX$it" }

    /**
     * Checks that all platform libs for [konanTarget] actually exist in the [distribution].
     */
    private fun checkLibrariesInDistribution(): Boolean {
        val presentPlatformLibs = platformLibsDirectory
            .listFiles { file -> file.isDirectory }.orEmpty()
            .map { it.name }.toSet()

        // TODO: Check that all directories in presentPlatformLibs are real klibs when klib componentization is merged.
        return presentDefs.toPlatformLibNames().all { it in presentPlatformLibs }
    }

    /**
     * Check that caches for all platform libs for [konanTarget] actually exist in the cache directory.
     */
    private fun checkCaches(): Boolean {
        if (!shouldBuildCaches) {
            return true
        }

        val cacheDirectory = CacheBuilder.getRootCacheDirectory(
            project.konanHome, konanTarget, true, konanCacheKind
        )
        return presentDefs.toPlatformLibNames().all {
            cacheDirectory.resolve(CacheBuilder.getCacheFileName(it, konanCacheKind)).listFilesOrEmpty().isNotEmpty()
        }
    }

    /**
     * We store directories where platform libraries were detected/generated earlier
     * during this build to avoid redundant distribution checks.
     */
    private val alreadyProcessed: PlatformLibsInfo
        get() = project.rootProject.extensions.extraProperties.run {
            if (!has(GENERATED_LIBS_PROPERTY_NAME)) {
                set(GENERATED_LIBS_PROPERTY_NAME, PlatformLibsInfo())
            }
            @Suppress("UNCHECKED_CAST")
            get(GENERATED_LIBS_PROPERTY_NAME) as PlatformLibsInfo
        }

    private fun runGenerationTool() = with(project) {
        val args = mutableListOf("-target", konanTarget.visibleName)
        if (logger.isInfoEnabled) {
            args.add("-verbose")
        }

        // We can generate caches using either [CacheBuilder] or the library generator. Using CacheBuilder allows
        // keeping all the caching logic in one place while the library generator speeds up building caches because
        // it works in parallel. We use the library generator for now due to performance reasons.
        //
        // TODO: Supporting Gradle Worker API (or other parallelization) in the CacheBuilder and enabling
        //       the compiler daemon for interop will allow switching to the CacheBuilder without performance penalty.
        //       Alternatively we can rely on the library generator tool in the CacheBuilder and eliminate a separate
        //       logic for library caching there.
        if (shouldBuildCaches) {
            args.addArg("-cache-kind", konanCacheKind.produce!!)
            args.addArg(
                "-cache-directory",
                CacheBuilder.getRootCacheDirectory(konanHome, konanTarget, true, konanCacheKind).absolutePath
            )
            args.addArg("-cache-arg", "-g")
            val additionalCacheFlags = konanPropertiesService.additionalCacheFlags(konanTarget)
            additionalCacheFlags.forEach {
                args.addArg("-cache-arg", it)
            }
        }
        KotlinNativeLibraryGenerationRunner.fromProject(this).run(args)
    }

    fun generatePlatformLibsIfNeeded(): Unit = with(project) {
        if (!HostManager(distribution).isEnabled(konanTarget)) {
            // We cannot generate libs on a machine that doesn't support the requested target.
            return
        }

        // Don't run the generator if libraries/caches for this target were already built during this Gradle invocation.
        val alreadyGenerated = alreadyProcessed.isGenerated(platformLibsDirectory)
        val alreadyCached = alreadyProcessed.isCached(platformLibsDirectory, konanCacheKind)
        if ((alreadyGenerated && alreadyCached) || !defDirectory.exists()) {
            return
        }

        // Check if libraries/caches for this target already exist (requires reading from disc).
        val platformLibsAreReady = checkLibrariesInDistribution()
        if (platformLibsAreReady) {
            alreadyProcessed.setGenerated(platformLibsDirectory)
        }

        val cachesAreReady = checkCaches()
        if (cachesAreReady) {
            alreadyProcessed.setCached(platformLibsDirectory, konanCacheKind)
        }

        val generationMessage = when {
            !platformLibsAreReady && !cachesAreReady ->
                "Generate and precompile platform libraries for $konanTarget (precompilation: ${konanCacheKind.visibleName})"
            platformLibsAreReady && !cachesAreReady ->
                "Precompile platform libraries for $konanTarget (precompilation: ${konanCacheKind.visibleName})"
            !platformLibsAreReady && cachesAreReady ->
                "Generate platform libraries for $konanTarget"
            else -> {
                // Both caches and libraries exist thus there is no need to run the generator.
                return
            }
        }

        logger.lifecycle(generationMessage)
        logger.lifecycleWithDuration("$generationMessage finished,") {
            runGenerationTool()
        }

        val librariesAreActuallyGenerated = checkLibrariesInDistribution()
        assert(librariesAreActuallyGenerated) { "Some platform libraries were not generated" }
        if (librariesAreActuallyGenerated) {
            alreadyProcessed.setGenerated(platformLibsDirectory)
        }

        val librariesAreActuallyCached = checkCaches()
        assert(librariesAreActuallyCached) { "Some platform libraries were not precompiled" }
        if (librariesAreActuallyCached) {
            alreadyProcessed.setCached(platformLibsDirectory, konanCacheKind)
        }
    }

    private class PlatformLibsInfo {
        private val generated: MutableSet<File> = Collections.newSetFromMap(ConcurrentHashMap<File, Boolean>())
        private val cached: ConcurrentHashMap<NativeCacheKind, MutableSet<File>> = ConcurrentHashMap()

        private fun cached(cacheKind: NativeCacheKind): MutableSet<File> = cached.getOrPut(cacheKind) {
            Collections.newSetFromMap(ConcurrentHashMap<File, Boolean>())
        }

        /**
         * Are platform libraries in the given directory (e.g. <dist>/klib/platform/ios_x64) generated.
         */
        fun isGenerated(path: File): Boolean =
            generated.contains(path)

        /**
         * Register that platform libraries in the given directory are generated.
         */
        fun setGenerated(path: File) {
            generated.add(path)
        }

        /**
         * Are platform libraries in the given directory (e.g. <dist>/klib/platform/ios_x64) cached with the given cache kind.
         */
        fun isCached(path: File, kind: NativeCacheKind): Boolean =
            kind == NativeCacheKind.NONE || cached(kind).contains(path)

        /**
         * Register that platform libraries in the give directory are cached with the given cache kind.
         */
        fun setCached(path: File, kind: NativeCacheKind) {
            if (kind != NativeCacheKind.NONE) {
                cached(kind).add(path)
            }
        }
    }

    companion object {
        private const val GENERATED_LIBS_PROPERTY_NAME = "org.jetbrains.kotlin.native.platform.libs.info"
    }
}