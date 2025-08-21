/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.settings

import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptionWithValue
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import org.jetbrains.kotlin.config.nativeBinaryOptions.GCSchedulerType
import org.jetbrains.kotlin.config.nativeBinaryOptions.parseBinaryOptions
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.konan.properties.resolvablePropertyList
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.MutedOption
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.RunnerWithExecutor
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.NoopTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.Runner
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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
enum class TestMode(private val description: String) {
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
class GCType(val gc: GC?) {
    val compilerFlag: String?
        get() = gc?.let { "-Xbinary=gc=${it.name.lowercase()}" }

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

class GCScheduler(val scheduler: GCSchedulerType?) {
    val compilerFlag: String?
        get() = scheduler?.let { "-Xbinary=gcSchedulerType=${it.name.lowercase()}" }

    override fun toString() = compilerFlag?.let { "($it)" }.orEmpty()
}

/**
 * Explicitly provided binary options.
 * See [org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions] for details.
 */
class ExplicitBinaryOptions(private val rawOptions: List<String>) {
    val options: List<BinaryOptionWithValue<*>> by lazy {
        parseBinaryOptions(rawOptions.toTypedArray(), { println(it) }, { error(it) })
    }

    inline fun <reified T> getOrNull(key: CompilerConfigurationKey<T>): T? =
        options.singleOrNull { it.compilerConfigurationKey == key }?.value as? T
}

enum class Allocator(val compilerFlag: String?) {
    UNSPECIFIED(null),
    STD("-Xallocator=std"),
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
class Timeouts(val executionTimeout: Duration) {
    companion object {
        val DEFAULT_EXECUTION_TIMEOUT: Duration get() = 10.minutes
    }
}

/**
 * Used cache mode.
 */
sealed class CacheMode {
    abstract val useStaticCacheForDistributionLibraries: Boolean
    abstract val useStaticCacheForUserLibraries: Boolean
    abstract val makePerFileCaches: Boolean
    abstract val useHeaders: Boolean
    abstract val alias: Alias

    object WithoutCache : CacheMode() {
        override val useStaticCacheForDistributionLibraries: Boolean = false
        override val useStaticCacheForUserLibraries: Boolean = false
        override val makePerFileCaches: Boolean = false
        override val useHeaders = false
        override val alias = Alias.NO
    }

    class WithStaticCache(
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

        override val useStaticCacheForDistributionLibraries: Boolean = true

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

            return when (kotlinNativeTargets.testTarget) {
                in cacheableTargets -> Alias.STATIC_ONLY_DIST
                // Support stdlib caches only for tests speedup
                KonanTarget.MINGW_X64 -> Alias.STATIC_ONLY_DIST
                else -> Alias.NO
            }
        }

        fun computeCacheDirName(
            testTarget: KonanTarget,
            cacheKind: String,
            debuggable: Boolean,
            partialLinkageEnabled: Boolean,
            checkStateAtExternalCalls: Boolean,
        ) = "$testTarget${if (debuggable) "-g" else ""}${cacheKind}${if (checkStateAtExternalCalls) "-check_state_at_external_calls" else ""}-user${if (partialLinkageEnabled) "-pl" else ""}"
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

enum class BinaryLibraryKind {
    STATIC, DYNAMIC
}

enum class KlibIrInlinerMode {
    OFF,
    ON,
}

internal enum class CInterfaceMode(val compilerFlag: String) {
    V1("-Xbinary=cInterfaceMode=v1"),
    NONE("-Xbinary=cInterfaceMode=none")
}

internal class XCTestRunner(val isEnabled: Boolean, private val nativeTargets: KotlinNativeTargets) {
    /**
     * Path to the developer frameworks directory.
     */
    val frameworksPath: String by lazy {
        "${targetPlatform()}/Developer/Library/Frameworks/"
    }

    private fun targetPlatform(): String {
        val xcodeTarget = when (val target = nativeTargets.testTarget) {
            KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64 -> "macosx"
            KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64 -> "iphonesimulator"
            KonanTarget.IOS_ARM64 -> "iphoneos"
            else -> error("Target $target is not supported buy the executor")
        }

        val result = try {
            runProcess(
                "/usr/bin/xcrun",
                "--sdk",
                xcodeTarget,
                "--show-sdk-platform-path"
            )
        } catch (t: Throwable) {
            throw IllegalStateException("Failed to run /usr/bin/xcrun process", t)
        }

        return result.stdout.trim()
    }
}

val Settings.systemFrameworksPath: String get() = get<XCTestRunner>().frameworksPath

/**
 * A custom (i.e., the second, an alternative) distribution of the Kotlin/Native compiler.
 * Intended for the use only in KLIB forward/backward compatibility tests where we need to compare
 * the compilation results of both the _current_ and the _custom_ compiler.
 */
internal class CustomNativeCompiler(private val lazyNativeHome: Lazy<KotlinNativeHome>) {
    val nativeHome: KotlinNativeHome get() = lazyNativeHome.value
    val lazyClassloader: Lazy<URLClassLoader> = lazy {
        val nativeClassPath = setOfNotNull(
            nativeHome.dir.resolve("konan/lib/trove4j.jar").takeIf {
                // This artifact was removed in Kotlin/Native 2.2.0-Beta1.
                // But it is still available in older compiler versions, where we need to load it.
                it.exists()
            },
            nativeHome.dir.resolve("konan/lib/kotlin-native-compiler-embeddable.jar")
        )
            .map { it.toURI().toURL() }
            .toTypedArray()

        URLClassLoader(nativeClassPath, null).apply { setDefaultAssertionStatus(true) }
    }
}