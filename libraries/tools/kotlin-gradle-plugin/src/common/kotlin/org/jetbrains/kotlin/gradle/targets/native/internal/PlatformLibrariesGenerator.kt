/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.compilerRunner.getKonanCacheKind
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.tasks.CacheBuilder
import org.jetbrains.kotlin.gradle.tasks.addArg
import org.jetbrains.kotlin.gradle.utils.lifecycleWithDuration
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeLibraryGenerationRunner
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeToolRunner
import org.jetbrains.kotlin.konan.library.KONAN_PLATFORM_LIBS_NAME_PREFIX
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.customerDistribution
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class PlatformLibrariesGenerator(
    objectFactory: ObjectFactory,
    val konanTarget: KonanTarget,
    private val propertiesProvider: PropertiesProvider,
    private val konanPropertiesService: Provider<KonanPropertiesBuildService>,
    metricsReporter: Provider<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>,
    classLoadersCachingService: Provider<ClassLoadersCachingBuildService>,
    private val platformLibrariesService: Provider<GeneratedPlatformLibrariesService>,
    useXcodeMessageStyle: Provider<Boolean>,
    private val nativeProperties: NativeProperties,
) {

    private val logger = Logging.getLogger(this::class.java)

    private val libraryGenerationRunner = objectFactory.KotlinNativeLibraryGenerationRunner(
        metricsReporter,
        classLoadersCachingService,
        useXcodeMessageStyle,
        nativeProperties,
        konanPropertiesService
    )

    private val konanHome
        get() = nativeProperties.actualNativeHomeDirectory.get()

    private val distribution = customerDistribution(
        konanHome.absolutePath,
        konanDataDir = nativeProperties.konanDataDir.orNull?.absolutePath
    )

    private val platformLibsDirectory =
        File(distribution.platformLibs(konanTarget)).absoluteFile

    private val defDirectory =
        File(distribution.platformDefs(konanTarget)).absoluteFile

    private val konanCacheKind: Provider<NativeCacheKind> = nativeProperties.getKonanCacheKind(konanTarget, konanPropertiesService)

    private val shouldBuildCaches: Boolean
        get() = konanPropertiesService.get().cacheWorksFor(konanTarget) &&
                konanCacheKind.get() != NativeCacheKind.NONE

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
            konanHome, konanTarget, true, konanCacheKind.get()
        )
        return presentDefs.toPlatformLibNames().all {
            cacheDirectory.resolve(CacheBuilder.getCacheFileName(it, konanCacheKind.get())).listFilesOrEmpty().isNotEmpty()
        }
    }

    /**
     * We store directories where platform libraries were detected/generated earlier
     * during this build to avoid redundant distribution checks.
     */
    private val alreadyProcessed: PlatformLibsInfo
        get() = platformLibrariesService.get().platformLibsInfo

    private fun runGenerationTool() {
        val args = mutableListOf("-target", konanTarget.visibleName)
        if (logger.isInfoEnabled) {
            args.add("-verbose")
        }
        nativeProperties.konanDataDir.orNull?.absolutePath?.let {
            args.addAll(listOf("-Xkonan-data-dir", it))
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
            args.addArg("-cache-kind", konanCacheKind.get().produce!!)
            args.addArg(
                "-cache-directory",
                CacheBuilder.getRootCacheDirectory(
                    nativeProperties.actualNativeHomeDirectory.get(),
                    konanTarget,
                    true,
                    konanCacheKind.get()
                ).absolutePath
            )
            args.addArg("-cache-arg", "-g")
            val additionalCacheFlags = konanPropertiesService.get().additionalCacheFlags(konanTarget)
            additionalCacheFlags.forEach {
                args.addArg("-cache-arg", it)
            }
        }
        libraryGenerationRunner.runTool(
            KotlinNativeToolRunner.ToolArguments(
                shouldRunInProcessMode = false,
                compilerArgumentsLogLevel = propertiesProvider.kotlinCompilerArgumentsLogLevel.get(),
                arguments = args
            )
        )
    }

    fun generatePlatformLibsIfNeeded() {
        if (!HostManager().isEnabled(konanTarget)) {
            // We cannot generate libs on a machine that doesn't support the requested target.
            return
        }

        // Don't run the generator if libraries/caches for this target were already built during this Gradle invocation.
        val alreadyGenerated = alreadyProcessed.isGenerated(platformLibsDirectory)
        val alreadyCached = alreadyProcessed.isCached(platformLibsDirectory, konanCacheKind.get())
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
            alreadyProcessed.setCached(platformLibsDirectory, konanCacheKind.get())
        }

        val generationMessage = when {
            !platformLibsAreReady && !cachesAreReady ->
                "Generate and precompile platform libraries for $konanTarget (precompilation: ${konanCacheKind.get().visibleName})"
            platformLibsAreReady && !cachesAreReady ->
                "Precompile platform libraries for $konanTarget (precompilation: ${konanCacheKind.get().visibleName})"
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
            alreadyProcessed.setCached(platformLibsDirectory, konanCacheKind.get())
        }
    }

    internal class PlatformLibsInfo {
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

    internal abstract class GeneratedPlatformLibrariesService : BuildService<BuildServiceParameters.None> {
        val platformLibsInfo = PlatformLibsInfo()
    }

    companion object {
        fun registerRequiredServiceIfAbsent(project: Project): Provider<GeneratedPlatformLibrariesService> {
            return project.gradle.registerClassLoaderScopedBuildService(GeneratedPlatformLibrariesService::class)
        }
    }
}
