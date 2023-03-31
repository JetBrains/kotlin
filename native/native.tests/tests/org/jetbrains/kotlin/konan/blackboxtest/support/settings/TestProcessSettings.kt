/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.konan.blackboxtest.support.MutedOption
import org.jetbrains.kotlin.konan.blackboxtest.support.TestKind
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.LocalTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.NoopTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.Runner
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The tested and the host Kotlin/Native targets.
 */
internal class KotlinNativeTargets(val testTarget: KonanTarget, val hostTarget: KonanTarget) {
    fun areDifferentTargets() = testTarget != hostTarget
}

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

internal class LLDB(nativeHome: KotlinNativeHome) {
    val prettyPrinters: File = nativeHome.dir.resolve("tools/konan_lldb.py")

    val isAvailable: Boolean by lazy {
        try {
            val exitCode = ProcessBuilder("lldb", "-version").start().waitFor()
            exitCode == 0
        } catch (e: IOException) {
            false
        }
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
 * The set of custom (external) klibs that should be passed to the Kotlin/Native compiler.
 */
@JvmInline
internal value class CustomKlibs(val klibs: Set<File>) {
    init {
        val invalidKlibs = klibs.filterNot { it.isDirectory || (it.isFile && it.extension == "klib") }
        assertTrue(invalidKlibs.isEmpty()) {
            "There are invalid KLIBs that should be passed for the Kotlin/Native compiler: ${klibs.joinToString { "[$it]" }}"
        }
    }
}

/**
 * Whether to force [TestKind.STANDALONE] for all tests where [TestKind] is assumed to be [TestKind.REGULAR] otherwise:
 * - either explicitly specified in the test data file: // KIND: REGULAR
 * - or // KIND: is not specified in the test data file and thus automatically considered as [TestKind.REGULAR]
 */
@JvmInline
internal value class ForcedStandaloneTestKind(val value: Boolean)

/**
 * Whether tests should be compiled only (true) or compiled and executed (false, the default).
 *
 * TODO: need to reconsider this setting when other [Runner]s than [LocalTestRunner] and [NoopTestRunner] are supported
 */
@JvmInline
internal value class ForcedNoopTestRunner(val value: Boolean)

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
    /**
     * but it should be done at some point.
     */
    LEGACY(listOf("-memory-model", "strict")),
    EXPERIMENTAL(listOf("-memory-model", "experimental"));

    override fun toString() = compilerFlags?.joinToString(prefix = "(", separator = " ", postfix = ")").orEmpty()
}

/**
 * Thread state checked. Can be applied only with [MemoryModel.EXPERIMENTAL], [OptimizationMode.DEBUG], [CacheMode.WithoutCache].
 */
internal enum class ThreadStateChecker(val compilerFlag: String?) {
    DISABLED(null),
    ENABLED("-Xcheck-state-at-external-calls");

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

/**
 * Type of sanitizer. Can be applied only with [CacheMode.WithoutCache]
 */
internal enum class Sanitizer(val compilerFlag: String?) {
    NONE(null),
    THREAD("-Xbinary=sanitizer=thread");

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
internal sealed class CacheMode {
    abstract val staticCacheForDistributionLibrariesRootDir: File?
    abstract val useStaticCacheForUserLibraries: Boolean
    abstract val makePerFileCaches: Boolean

    val useStaticCacheForDistributionLibraries: Boolean get() = staticCacheForDistributionLibrariesRootDir != null

    object WithoutCache : CacheMode() {
        override val staticCacheForDistributionLibrariesRootDir: File? get() = null
        override val useStaticCacheForUserLibraries: Boolean get() = false
        override val makePerFileCaches: Boolean = false
    }

    class WithStaticCache(
        distribution: Distribution,
        kotlinNativeTargets: KotlinNativeTargets,
        optimizationMode: OptimizationMode,
        override val useStaticCacheForUserLibraries: Boolean,
        override val makePerFileCaches: Boolean
    ) : CacheMode() {
        override val staticCacheForDistributionLibrariesRootDir: File = File(distribution.klib)
            .resolve("cache")
            .resolve(
                computeDistroCacheDirName(
                    testTarget = kotlinNativeTargets.testTarget,
                    cacheKind = CACHE_KIND,
                    debuggable = optimizationMode == OptimizationMode.DEBUG
                )
            ).apply {
                assertTrue(exists()) { "The distribution libraries cache directory is not found: $this" }
                assertTrue(isDirectory) { "The distribution libraries cache directory is not a directory: $this" }
                assertTrue(list().orEmpty().isNotEmpty()) { "The distribution libraries cache directory is empty: $this" }
            }

        companion object {
            private const val CACHE_KIND = "STATIC"
        }
    }

    enum class Alias { NO, STATIC_ONLY_DIST, STATIC_EVERYWHERE, STATIC_PER_FILE_EVERYWHERE }

    companion object {
        fun defaultForTestTarget(distribution: Distribution, kotlinNativeTargets: KotlinNativeTargets): Alias {
            val cacheableTargets = distribution.properties
                .resolvablePropertyList("cacheableTargets", kotlinNativeTargets.hostTarget.name)
                .map { KonanTarget.predefinedTargets.getValue(it) }
                .toSet()

            return if (kotlinNativeTargets.testTarget in cacheableTargets) Alias.STATIC_ONLY_DIST else Alias.NO
        }

        fun computeCacheDirName(
            testTarget: KonanTarget,
            cacheKind: String,
            debuggable: Boolean,
            partialLinkageEnabled: Boolean
        ) = "$testTarget${if (debuggable) "-g" else ""}$cacheKind${if (partialLinkageEnabled) "-pl" else ""}"

        // N.B. The distribution libs are always built with the partial linkage turned off.
        fun computeDistroCacheDirName(
            testTarget: KonanTarget,
            cacheKind: String,
            debuggable: Boolean,
        ) = "$testTarget${if (debuggable) "-g" else ""}$cacheKind"
    }
}

internal enum class PipelineType(val mutedOption: MutedOption, val compilerFlags: List<String>) {
    K1(MutedOption.K1, emptyList()),
    K2(MutedOption.K2, listOf("-language-version", "2.0"));

    override fun toString() = if (compilerFlags.isEmpty()) "" else compilerFlags.joinToString(prefix = "(", postfix = ")", separator = " ")
}

internal enum class CompilerOutputInterceptor {
    DEFAULT,
    NONE
}
