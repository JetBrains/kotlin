/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.util.TestDisposable
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File
import java.net.URLClassLoader

internal class TestEnvironment(
    val globalEnvironment: GlobalTestEnvironment,
    val testRoots: TestRoots, // The directories with original sources (aka testData).
    val testSourcesDir: File, // The directory with generated (preprocessed) test sources.
    val sharedSourcesDir: File, // The directory with the sources of the shared modules (i.e. the modules that are widely used in multiple tests).
    val testBinariesDir: File, // The directory with compiled test binaries (klibs) and executable files).
    val sharedBinariesDir: File // The directory with compiled shared modules (klibs).
) : TestDisposable(parentDisposable = null)

internal class GlobalTestEnvironment(
    val target: KonanTarget = HostManager.host,
    val kotlinNativeHome: File = defaultKotlinNativeHome,
    val lazyKotlinNativeClassLoader: Lazy<ClassLoader> = defaultKotlinNativeClassLoader,
    val testMode: TestMode = defaultTestMode,
    val cacheSettings: TestCacheSettings = defaultCacheSettings,
    val baseBuildDir: File = projectBuildDir
) {
    val hostTarget: KonanTarget = HostManager.host

    fun getRootCacheDirectory(debuggable: Boolean): File? =
        (cacheSettings as? TestCacheSettings.WithCache)?.getRootCacheDirectory(this, debuggable)

    companion object {
        private val defaultKotlinNativeHome: File
            get() = System.getProperty(KOTLIN_NATIVE_HOME)?.let(::File) ?: fail { "Non-specified $KOTLIN_NATIVE_HOME system property" }

        // Use isolated cached class loader.
        private val defaultKotlinNativeClassLoader: Lazy<ClassLoader> = lazy {
            val nativeClassPath = System.getProperty(COMPILER_CLASSPATH)
                ?.split(':', ';')
                ?.map { File(it).toURI().toURL() }
                ?.toTypedArray()
                ?: fail { "Non-specified $COMPILER_CLASSPATH system property" }

            URLClassLoader(nativeClassPath, /* no parent class loader */ null).apply { setDefaultAssertionStatus(true) }
        }

        private val defaultTestMode: TestMode = run {
            val testModeName = System.getProperty(TEST_MODE) ?: return@run TestMode.WITH_MODULES

            TestMode.values().firstOrNull { it.name == testModeName } ?: fail {
                buildString {
                    appendLine("Unknown test mode name $testModeName.")
                    appendLine("One of the following test modes should be passed through $TEST_MODE system property:")
                    TestMode.values().forEach { testMode ->
                        appendLine("- ${testMode.name}: ${testMode.description}")
                    }
                }
            }
        }

        private val defaultCacheSettings: TestCacheSettings = run {
            val useCacheValue = System.getProperty(USE_CACHE)
            val useCache = if (useCacheValue != null) {
                useCacheValue.toBooleanStrictOrNull() ?: fail { "Invalid value for $USE_CACHE system property: $useCacheValue" }
            } else
                true

            if (useCache) TestCacheSettings.WithCache else TestCacheSettings.WithoutCache
        }

        private val projectBuildDir: File
            get() = System.getenv(PROJECT_BUILD_DIR)?.let(::File) ?: fail { "Non-specified $PROJECT_BUILD_DIR environment variable" }

        private const val KOTLIN_NATIVE_HOME = "kotlin.internal.native.test.nativeHome"
        private const val COMPILER_CLASSPATH = "kotlin.internal.native.test.compilerClasspath"
        private const val TEST_MODE = "kotlin.internal.native.test.mode"
        private const val USE_CACHE = "kotlin.internal.native.test.useCache"
        private const val PROJECT_BUILD_DIR = "PROJECT_BUILD_DIR"
    }
}

internal class TestRoots(
    val roots: Set<File>,
    val baseDir: File
)

// TODO: in fact, only WITH_MODULES mode is supported now
internal enum class TestMode(val description: String) {
    ONE_STAGE(
        description = "Compile test files altogether without producing intermediate KLIBs."
    ),
    TWO_STAGE(
        description = "Compile test files altogether and produce an intermediate KLIB. Then produce a program from the KLIB using -Xinclude."
    ),
    WITH_MODULES(
        description = "Compile each test file as one or many modules (depending on MODULE directives declared in the file)." +
                " Then link the KLIBs into the single executable file."
    )
}

internal sealed interface TestCacheSettings {
    object WithoutCache : TestCacheSettings

    object WithCache : TestCacheSettings {
        fun getRootCacheDirectory(globalEnvironment: GlobalTestEnvironment, debuggable: Boolean): File? = with(globalEnvironment) {
            kotlinNativeHome.resolve("klib/cache").resolve(getCacheDirName(target, debuggable)).takeIf { it.exists() }
        }

        private const val DEFAULT_CACHE_KIND = "STATIC"

        private fun getCacheDirName(target: KonanTarget, debuggable: Boolean) = "$target${if (debuggable) "-g" else ""}$DEFAULT_CACHE_KIND"
    }
}
