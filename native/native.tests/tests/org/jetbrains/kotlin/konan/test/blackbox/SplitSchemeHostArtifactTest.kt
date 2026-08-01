/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.SplitSchemeHostArtifactTest.Companion.TEST_SUITE_PATH
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("caches")
@EnforcedHostTarget
@TestMetadata(TEST_SUITE_PATH)
@TestDataPath($$"$PROJECT_ROOT")
class SplitSchemeHostArtifactTest : AbstractNativeSimpleTest() {

    companion object {
        const val TEST_SUITE_PATH = "native/native.tests/testData/splitScheme"

        // Caches force-loaded straight into the host (see BootstrapMetadata.FORCE_LOADED_CACHES_FQN);
        // they are linked directly, so their paths are NOT embedded in the runtime manifest.
        private val FORCE_LOADED = listOf("kotlin.native.internal", "skiko", "libkotlin", "libstdlib-cache")

        private val USER_SYMBOLS = listOf("SplitSchemeProbe", "topLevelProbeEntry")
    }

    @BeforeEach
    fun assumeCachesAreEnabled() {
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
        Assumptions.assumeFalse(targets.testTarget == KonanTarget.MINGW_X64)
    }

    @Test
    @TestMetadata("symbolConsistency")
    fun testHostExcludesUserCodeAndEmbedsRuntimeCachePaths() {
        val rootDir = ForTestCompileRuntime.transformTestDataPath("$TEST_SUITE_PATH/symbolConsistency")
        val icCacheDir = buildDir.resolve("ic_cache").also { it.mkdirs() }

        val lib = compileToLibrary(rootDir.resolve("lib"))
        val host = compileToExecutableInOneStage(
            rootDir.resolve("main"),
            tryPassSystemCacheDirectory = false,
            freeCompilerArgs = TestCompilerArgs(
                [
                    "-Xverbose-phases=Linker",
                    "-Xcompilation-scheme=split",
                    "-g",
                    "-Xbinary=perFileCacheForStdlib=false",
                    "-Xenable-incremental-compilation",
                    "-Xic-cache-dir=${icCacheDir.absolutePath}",
                ]
            ),
            lib,
        ).executableFile

        val stringsDefinedInExecutable = strings(host)

        // User code was split out into the bootstrap object, so the host binary must not contain it.
        USER_SYMBOLS.forEach { symbol ->
            assertFalse(stringsDefinedInExecutable.any { symbol in it }, "host binary must not contain user symbol '$symbol'")
        }

        // The runtime manifest embeds absolute paths to the caches the host loads at startup,
        // exactly the ones that are not force-loaded.
        val cachePaths = stringsDefinedInExecutable.filter { "/" in it && it.endsWith(".a") }
        assertTrue(
            cachePaths.any { path -> FORCE_LOADED.none { it in path } },
            "expected a non-force-loaded cache path embedded in the host binary, found: $cachePaths"
        )
    }

    private fun strings(file: File): List<String> {
        val llvmStrings = "${testRunSettings.configurables.absoluteLlvmHome}/bin/llvm-strings"
        val process = ProcessBuilder(llvmStrings, file.absolutePath).redirectErrorStream(true).start()
        val lines = process.inputStream.bufferedReader().readLines()
        check(process.waitFor() == 0) { "`$llvmStrings ${file.absolutePath}` exited with a non-zero code" }
        return lines
    }
}
