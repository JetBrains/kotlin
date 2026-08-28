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
    }

    @BeforeEach
    fun assumeCachesAreEnabled() {
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
        Assumptions.assumeFalse(targets.testTarget == KonanTarget.MINGW_X64)
    }

    @Test
    @TestMetadata("forceLinkCachePackages")
    fun testForceLinkCachePackagesLinksMatchingFacadeIntoHost() {
        val rootDir = ForTestCompileRuntime.transformTestDataPath("$TEST_SUITE_PATH/forceLinkCachePackages")
        val facade = compileToLibrary(
            rootDir.resolve("facade"),
            buildDir,
            TestCompilerArgs("-manifest", rootDir.resolve("facade.properties").absolutePath),
            emptyList(),
        )

        fun hostSymbols(forceLinkCachePackage: String, cacheDirName: String): List<String> {
            val icCacheDir = buildDir.resolve(cacheDirName).also { it.mkdirs() }
            val host = compileToExecutableInOneStage(
                rootDir.resolve("main"),
                tryPassSystemCacheDirectory = false,
                freeCompilerArgs = TestCompilerArgs(
                    [
                        "-Xverbose-phases=Linker",
                        "-Xcompilation-scheme=split",
                        "-Xsplit-force-link-cache-packages=$forceLinkCachePackage",
                        "-g",
                        "-Xbinary=perFileCacheForStdlib=false",
                        "-Xenable-incremental-compilation",
                        "-Xic-cache-dir=${icCacheDir.absolutePath}",
                    ]
                ),
                facade,
            ).executableFile
            return host.definedSymbols
        }

        val symbolsWithUnmatchedFqn = hostSymbols(
            forceLinkCachePackage = "org.jetbrains.kotlin.native.test.unmatched",
            cacheDirName = "unmatched_ic_cache",
        )
        val unmatchedFacadeSymbols = symbolsWithUnmatchedFqn.filter { "facadeProbe" in it }
        assertFalse(
            unmatchedFacadeSymbols.isNotEmpty(),
            "the facade cache must not be linked into the host for an unmatched package FQN: $unmatchedFacadeSymbols",
        )

        val symbolsWithFacadeFqn = hostSymbols(
            forceLinkCachePackage = "org.jetbrains.kotlin.native.test.facade",
            cacheDirName = "facade_ic_cache",
        )
        val matchingFacadeSymbols = symbolsWithFacadeFqn.filter { "facadeProbe" in it }
        assertTrue(
            matchingFacadeSymbols.isNotEmpty(),
            "expected the matching facade cache symbols in the host binary: $matchingFacadeSymbols",
        )
    }

    private val File.definedSymbols: List<String>
        get() {
            val llvmNm = "${testRunSettings.configurables.absoluteLlvmHome}/bin/llvm-nm"
            val process = ProcessBuilder(llvmNm, "--defined-only", this.absolutePath)
                .redirectErrorStream(true)
                .start()
            val lines = process.inputStream.bufferedReader().readLines()
            check(process.waitFor() == 0) {
                "`$llvmNm --defined-only ${this.absolutePath}` exited with a non-zero code"
            }
            return lines
        }
}
