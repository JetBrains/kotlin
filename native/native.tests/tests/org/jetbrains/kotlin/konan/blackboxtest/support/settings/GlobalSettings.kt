/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import java.io.File
import java.net.URLClassLoader
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class GlobalSettings(
    val target: KonanTarget = HostManager.host,
    val kotlinNativeHome: File = defaultKotlinNativeHome,
    val lazyKotlinNativeClassLoader: Lazy<ClassLoader> = defaultKotlinNativeClassLoader,
    val testMode: TestMode = defaultTestMode,
    val cacheSettings: CacheSettings = defaultCacheSettings,
    val executionTimeout: Duration = defaultExecutionTimeout,
    val baseBuildDir: File = projectBuildDir
) {
    val hostTarget: KonanTarget = HostManager.host

    fun getRootCacheDirectory(debuggable: Boolean): File? =
        (cacheSettings as? CacheSettings.WithCache)?.getRootCacheDirectory(this, debuggable)

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

        private val defaultCacheSettings: CacheSettings = run {
            val useCacheValue = System.getProperty(USE_CACHE)
            val useCache = if (useCacheValue != null) {
                useCacheValue.toBooleanStrictOrNull() ?: fail { "Invalid value for $USE_CACHE system property: $useCacheValue" }
            } else
                true

            if (useCache) CacheSettings.WithCache else CacheSettings.WithoutCache
        }

        private val defaultExecutionTimeout: Duration = run {
            val executionTimeoutValue = System.getProperty(EXECUTION_TIMEOUT)
            if (executionTimeoutValue != null) {
                executionTimeoutValue.toLongOrNull()?.milliseconds
                    ?: fail { "Invalid value for $EXECUTION_TIMEOUT system property: $executionTimeoutValue" }
            } else
                DEFAULT_EXECUTION_TIMEOUT
        }

        private val projectBuildDir: File
            get() = System.getenv(PROJECT_BUILD_DIR)?.let(::File) ?: fail { "Non-specified $PROJECT_BUILD_DIR environment variable" }

        private const val KOTLIN_NATIVE_HOME = "kotlin.internal.native.test.nativeHome"
        private const val COMPILER_CLASSPATH = "kotlin.internal.native.test.compilerClasspath"
        private const val TEST_MODE = "kotlin.internal.native.test.mode"
        private const val USE_CACHE = "kotlin.internal.native.test.useCache"
        private const val EXECUTION_TIMEOUT = "kotlin.internal.native.test.executionTimeout"
        private const val PROJECT_BUILD_DIR = "PROJECT_BUILD_DIR"

        private val DEFAULT_EXECUTION_TIMEOUT get() = 10.seconds // Use no backing field to avoid null-initialized value.
    }
}

internal sealed interface CacheSettings {
    object WithoutCache : CacheSettings

    object WithCache : CacheSettings {
        fun getRootCacheDirectory(settings: GlobalSettings, debuggable: Boolean): File? = with(settings) {
            kotlinNativeHome.resolve("klib/cache").resolve(getCacheDirName(target, debuggable)).takeIf { it.exists() }
        }

        private const val DEFAULT_CACHE_KIND = "STATIC"

        private fun getCacheDirName(target: KonanTarget, debuggable: Boolean) = "$target${if (debuggable) "-g" else ""}$DEFAULT_CACHE_KIND"
    }
}
