/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The tested and the host Kotlin/Native targets.
 */
internal class KotlinNativeTargets(val testTarget: KonanTarget, val hostTarget: KonanTarget)

/**
 * The Kotlin/Native home.
 */
internal class KotlinNativeHome(val dir: File) {
    val librariesDir: File = dir.resolve("klib")
    val stdlibFile: File = librariesDir.resolve("common/stdlib")

    val properties: Properties by lazy {
        dir.resolve("konan/konan.properties").inputStream().use { Properties().apply { load(it) } }
    }
}

/**
 * Lazy-initialized class loader with the Kotlin/Native embedded compiler.
 */
internal class KotlinNativeClassLoader(private val lazyClassLoader: Lazy<ClassLoader>) {
    val classLoader: ClassLoader get() = lazyClassLoader.value
}

/**
 * New test modes may be added as necessary.
 */
internal enum class TestMode(private val description: String) {
    ONE_STAGE_MULTI_MODULE(
        description = "Compile each test file as one or many modules (depending on MODULE directives declared in the file)." +
                " Produce a KLIB per each module except the last one." +
                " Finally, produce an executable file by compiling the latest module with all other KLIBs passed as -library"
    ),
    TWO_STAGE_MULTI_MODULE(
        description = "Compile each test file as one or many modules (depending on MODULE directives declared in the file)." +
                " Produce a KLIB per each module." +
                " Finally, produce an executable file by passing the latest KLIB as -Xinclude and all other KLIBs as -library."
    );

    override fun toString() = description
}

/**
 * Optimization mode to be applied.
 */
internal enum class OptimizationMode(private val description: String, val compilerFlag: String?) {
    DEBUG("Build with debug information", "-g"),
    OPT("Build with optimizations applied", "-opt"),
    NO("Don't use any specific optimizations", null);

    override fun toString() = description + compilerFlag?.let { " ($it)" }.orEmpty()
}

/**
 * The Kotlin/Native memory model.
 */
internal enum class MemoryModel(val compilerFlags: List<String>?) {
    DEFAULT(null),
    EXPERIMENTAL(listOf("-memory-model", "experimental"));

    override fun toString() = compilerFlags?.joinToString(prefix = "(", separator = " ", postfix = ")").orEmpty()
}

/**
 * Thread state checked. Can be applied only with [MemoryModel.EXPERIMENTAL] and [OptimizationMode.DEBUG].
 */
internal enum class ThreadStateChecker(val compilerFlag: String?) {
    DISABLED(null),
    ENABLED("-Xcheck-state-at-external-calls");

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

/**
 * Garbage collector type. Can be applied only with [MemoryModel.EXPERIMENTAL].
 */
internal enum class GCType(val compilerFlag: String?) {
    UNSPECIFIED(null),
    NOOP("-Xgc=noop"),
    STMS("-Xgc=stms"),
    CMS("-Xgc=cms");

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

internal enum class GCScheduler(val compilerFlag: String?) {
    UNSPECIFIED(null),
    DISABLED("-Xbinary=gcSchedulerType=disabled"),
    WITH_TIMER("-Xbinary=gcSchedulerType=with_timer"),
    ON_SAFE_POINTS("-Xbinary=gcSchedulerType=on_safe_points"),
    AGGRESSIVE("-Xbinary=gcSchedulerType=aggressive");

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

/**
 * Current project's directories.
 */
internal class BaseDirs(val testBuildDir: File)

/**
 * Timeouts.
 */
internal class Timeouts(val executionTimeout: Duration) {
    companion object {
        val DEFAULT_EXECUTION_TIMEOUT: Duration get() = 15.seconds
    }
}

/**
 * Used cache mode.
 */
internal sealed interface CacheMode {
    val staticCacheRootDir: File?
    val staticCacheRequiredForEveryLibrary: Boolean

    object WithoutCache : CacheMode {
        override val staticCacheRootDir: File? get() = null
        override val staticCacheRequiredForEveryLibrary get() = false
    }

    class WithStaticCache(
        kotlinNativeHome: KotlinNativeHome,
        kotlinNativeTargets: KotlinNativeTargets,
        optimizationMode: OptimizationMode,
        override val staticCacheRequiredForEveryLibrary: Boolean
    ) : CacheMode {
        override val staticCacheRootDir: File = kotlinNativeHome.dir
            .resolve("klib/cache")
            .resolve(
                computeCacheDirName(
                    testTarget = kotlinNativeTargets.testTarget,
                    cacheKind = CACHE_KIND,
                    debuggable = optimizationMode == OptimizationMode.DEBUG
                )
            ).also { rootCacheDir ->
                assertTrue(rootCacheDir.exists()) { "The root cache directory is not found: $rootCacheDir" }
                assertTrue(rootCacheDir.isDirectory) { "The root cache directory is not a directory: $rootCacheDir" }
                assertTrue(rootCacheDir.list().orEmpty().isNotEmpty()) { "The root cache directory is empty: $rootCacheDir" }
            }

        companion object {
            private const val CACHE_KIND = "STATIC"
        }
    }

    enum class Alias { NO, STATIC_ONLY_DIST, STATIC_EVERYWHERE }

    companion object {
        private fun computeCacheDirName(testTarget: KonanTarget, cacheKind: String, debuggable: Boolean) =
            "$testTarget${if (debuggable) "-g" else ""}$cacheKind"
    }
}
