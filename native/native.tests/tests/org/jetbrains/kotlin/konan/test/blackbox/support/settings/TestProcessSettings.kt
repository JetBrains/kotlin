/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.settings

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.MutedOption
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.RunnerWithExecutor
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.NoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.Runner
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The tested and the host Kotlin/Native targets.
 */
class KotlinNativeTargets(val testTarget: KonanTarget, val hostTarget: KonanTarget) {
    fun areDifferentTargets() = testTarget != hostTarget
}

/**
 * The Kotlin/Native home.
 */
class KotlinNativeHome(val dir: File) {
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
class KotlinNativeClassLoader(private val lazyClassLoader: Lazy<ClassLoader>) {
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
 * Kotlin compiler plugins to be used together with the the Kotlin/Native compiler.
 */
@JvmInline
value class CompilerPlugins(val compilerPluginJars: Set<File>) {
    init {
        val invalidJars = compilerPluginJars.filterNot { it.isDirectory || (it.isFile && it.extension == "jar") }
        assertTrue(invalidJars.isEmpty()) {
            "There are invalid compiler plugin JARs that should be passed for the Kotlin/Native compiler: ${invalidJars.joinToString { "[$it]" }}"
        }
    }
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
 * Whether tests should be compiled only (true) or compiled and executed (false, the default).
 *
 * TODO: need to reconsider this setting when other [Runner]s than [RunnerWithExecutor] and [NoopTestRunner] are supported
 */
@JvmInline
internal value class ForcedNoopTestRunner(val value: Boolean)

/**
 * Controls whether tests that support TestRunner should be executed once in the binary.
 * Their execution result is shared between tests from the same test executable.
 */
@JvmInline
internal value class SharedExecutionTestRunner(val value: Boolean)

/**
 * Optimization mode to be applied.
 */
enum class OptimizationMode(private val description: String, val compilerFlag: String?) {
    DEBUG("Build with debug information", "-g"),
    OPT("Build with optimizations applied", "-opt"),
    NO("Don't use any specific optimizations", null);

    override fun toString() = description + compilerFlag?.let { " ($it)" }.orEmpty()
}

/**
 * Thread state checked. Can be applied only with [OptimizationMode.DEBUG], [CacheMode.WithoutCache].
 */
enum class ThreadStateChecker(val compilerFlag: String?) {
    DISABLED(null),
    ENABLED("-Xbinary=checkStateAtExternalCalls=true");

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

/**
 * Type of sanitizer. Can be applied only with [CacheMode.WithoutCache]
 */
enum class Sanitizer(val compilerFlag: String?) {
    NONE(null),
    THREAD("-Xbinary=sanitizer=thread");

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

/**
 * Garbage collector type.
 */
enum class GCType(val compilerFlag: String?) {
    UNSPECIFIED(null),
    NOOP("-Xbinary=gc=noop"),
    STWMS("-Xbinary=gc=stwms"),
    PMCS("-Xbinary=gc=pmcs"),
    CMS("-Xbinary=gc=cms");

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

enum class GCScheduler(val compilerFlag: String?) {
    UNSPECIFIED(null),
    MANUAL("-Xbinary=gcSchedulerType=manual"),
    ADAPTIVE("-Xbinary=gcSchedulerType=adaptive"),
    AGGRESSIVE("-Xbinary=gcSchedulerType=aggressive"),

    // TODO: Remove these deprecated GC scheduler options.
    DISABLED("-Xbinary=gcSchedulerType=disabled"),
    WITH_TIMER("-Xbinary=gcSchedulerType=with_timer"),
    ON_SAFE_POINTS("-Xbinary=gcSchedulerType=on_safe_points");

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

enum class Allocator(val compilerFlag: String?) {
    UNSPECIFIED(null),
    STD("-Xallocator=std"),
    MIMALLOC("-Xallocator=mimalloc"),
    CUSTOM("-Xallocator=custom");

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
        val DEFAULT_EXECUTION_TIMEOUT: Duration get() = 30.seconds
    }
}

/**
 * Used cache mode.
 */
sealed class CacheMode {
    abstract val staticCacheForDistributionLibrariesRootDir: File?
    abstract val useStaticCacheForUserLibraries: Boolean
    abstract val makePerFileCaches: Boolean
    abstract val useHeaders: Boolean
    abstract val alias: Alias

    val useStaticCacheForDistributionLibraries: Boolean get() = staticCacheForDistributionLibrariesRootDir != null

    object WithoutCache : CacheMode() {
        override val staticCacheForDistributionLibrariesRootDir: File? get() = null
        override val useStaticCacheForUserLibraries: Boolean get() = false
        override val makePerFileCaches: Boolean = false
        override val useHeaders = false
        override val alias = Alias.NO
    }

    class WithStaticCache(
        distribution: Distribution,
        kotlinNativeTargets: KotlinNativeTargets,
        optimizationMode: OptimizationMode,
        override val useStaticCacheForUserLibraries: Boolean,
        override val makePerFileCaches: Boolean,
        override val useHeaders: Boolean,
        override val alias: Alias,
    ) : CacheMode() {
        init {
            assertFalse (optimizationMode == OptimizationMode.OPT) {
                "Static caches are incompatible with `-P${ClassLevelProperty.OPTIMIZATION_MODE.propertyName}=${OptimizationMode.OPT.name}`.\n" +
                "To test in ${OptimizationMode.OPT.name} mode, either don't specify `-P${ClassLevelProperty.CACHE_MODE.propertyName}`, or set it to ${Alias.NO.name}."
            }
        }

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

    enum class Alias { NO, STATIC_ONLY_DIST, STATIC_EVERYWHERE, STATIC_PER_FILE_EVERYWHERE, STATIC_USE_HEADERS_EVERYWHERE }

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

enum class PipelineType(val mutedOption: MutedOption, val compilerFlags: List<String>) {
    DEFAULT(
        MutedOption.DEFAULT,
        emptyList()
    ),
    K1(
        MutedOption.K1,
        listOf("-language-version", "1.9")
    ),
    K2(
        MutedOption.K2,
        listOf("-language-version", if (LanguageVersion.LATEST_STABLE.major < 2) "2.0" else LanguageVersion.LATEST_STABLE.toString())
    );

    override fun toString() = if (compilerFlags.isEmpty()) "" else compilerFlags.joinToString(prefix = "(", postfix = ")", separator = " ")
}

enum class CompilerOutputInterceptor {
    DEFAULT,
    NONE
}

internal enum class TestGroupCreation {
    DEFAULT,
    EAGER;

    companion object {
        private const val PROPERTY = "kotlin.internal.native.test.eagerGroupCreation"

        fun getFromProperty(): TestGroupCreation = System.getProperty(PROPERTY)
            ?.let {
                if (it.toBoolean()) EAGER
                else DEFAULT
            } ?: DEFAULT
    }
}

internal enum class BinaryLibraryKind {
    STATIC, DYNAMIC
}

internal enum class CInterfaceMode(val compilerFlag: String) {
    V1("-Xbinary=cInterfaceMode=v1"),
    NONE("-Xbinary=cInterfaceMode=none")
}
