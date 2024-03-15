/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.test.blackbox.CachesAutoBuildTest.Companion.TEST_SUITE_PATH
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.UsedPartialLinkageConfig
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("caches")
@EnforcedHostTarget
@TestMetadata(TEST_SUITE_PATH)
@TestDataPath("\$PROJECT_ROOT")
class CachesAutoBuildTest : AbstractNativeSimpleTest() {

    @BeforeEach
    fun assumeCachesAreEnabled() {
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
    }

    @Test
    @TestMetadata("simple")
    fun testSimple() {
        val rootDir = File("$TEST_SUITE_PATH/simple")
        val lib = compileToLibrary(rootDir.resolve("lib"), buildDir)
        val main = compileToExecutable(rootDir.resolve("main"), autoCacheFrom = buildDir, emptyList(), emptyList(), lib)

        assertTrue(main.exists())
        assertTrue(autoCacheDir.resolve(cacheFlavor).resolve("lib").exists())
    }

    @Test
    @TestMetadata("dontCacheUserLib")
    fun testDontCacheUserLib() {
        val rootDir = File("$TEST_SUITE_PATH/dontCacheUserLib")
        val externalLib = compileToLibrary(rootDir.resolve("externalLib"), buildDir.resolve("external"))
        val userLib = compileToLibrary(rootDir.resolve("userLib"), buildDir.resolve("user"), externalLib)
        val main = compileToExecutable(
            rootDir.resolve("main"),
            autoCacheFrom = buildDir.resolve("external"), emptyList(), emptyList(),
            externalLib, userLib
        )

        assertTrue(main.exists())
        assertTrue(autoCacheDir.resolve(cacheFlavor).resolve("externalLib").exists())
        assertFalse(autoCacheDir.resolve(cacheFlavor).resolve("userLib").exists())
    }

    @Test
    @TestMetadata("cacheDirPrioritizesOverAutoCacheDir")
    fun testCacheDirPrioritizesOverAutoCacheDir() {
        val rootDir = File("$TEST_SUITE_PATH/simple")
        val lib = compileToLibrary(rootDir.resolve("lib"), buildDir)
        val cacheDir = buildDir.resolve("lib_cache")
        cacheDir.mkdirs()
        compileToStaticCache(lib, cacheDir)
        val makePerFileCache = testRunSettings.get<CacheMode>().makePerFileCaches
        assertTrue(cacheDir.resolve("lib-${if (makePerFileCache) "per-file-cache" else "cache"}").exists())
        val main = compileToExecutable(rootDir.resolve("main"), autoCacheFrom = buildDir, listOf(cacheDir), emptyList(), lib)

        assertTrue(main.exists())
        assertFalse(autoCacheDir.resolve(cacheFlavor).resolve("lib").exists())
    }

    @Test
    @TestMetadata("testCustomBinaryOptions")
    fun testCustomBinaryOptions() {
        val rootDir = File("$TEST_SUITE_PATH/simple")
        val lib = compileToLibrary(rootDir.resolve("lib"), buildDir)
        val main = compileToExecutable(rootDir.resolve("main"), autoCacheFrom = buildDir, emptyList(), emptyList(), lib)

        assertTrue(main.exists())
        assertTrue(autoCacheDir.resolve(cacheFlavor).resolve("lib").exists())

        val customRuntimeAsserts = "-Xbinary=runtimeAssertionsMode=log"
        val main2 = compileToExecutable(rootDir.resolve("main"), autoCacheFrom = buildDir, emptyList(), listOf(customRuntimeAsserts), lib)

        assertTrue(main2.exists())
    }

    private fun compileToExecutable(
        sourcesDir: File,
        autoCacheFrom: File,
        cacheDirectories: List<File>,
        additionalArgs: List<String>,
        vararg dependencies: KLIB
    ): File {
        autoCacheDir.mkdirs()
        return compileToExecutableInOneStage(
            sourcesDir,
            tryPassSystemCacheDirectory = false, // With auto-cache mode, the compiler chooses the system cache directory itself.
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xauto-cache-from=${autoCacheFrom.absolutePath}",
                    "-Xauto-cache-dir=${autoCacheDir.absolutePath}",
                ) + cacheDirectories.map { "-Xcache-directory=${it.absolutePath}" } + additionalArgs
            ),
            *dependencies
        ).executableFile
    }

    private val autoCacheDir: File get() = buildDir.resolve("__auto_cache__")
    private val cacheFlavor: String
        get() = CacheMode.computeCacheDirName(
            testRunSettings.get<KotlinNativeTargets>().testTarget,
            "STATIC",
            testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG,
            partialLinkageEnabled = testRunSettings.get<UsedPartialLinkageConfig>().config.isEnabled
        )

    companion object {
        const val TEST_SUITE_PATH = "native/native.tests/testData/caches/testAutoBuild"
    }
}
