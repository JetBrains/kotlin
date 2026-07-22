/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.CachesSplitSchemeBuildTest.Companion.TEST_SUITE_PATH
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test


@Tag("caches")
@EnforcedHostTarget
@TestMetadata(TEST_SUITE_PATH)
@TestDataPath($$"$PROJECT_ROOT")
class CachesSplitSchemeBuildTest : AbstractNativeSimpleTest() {

    companion object {
        const val TEST_SUITE_PATH = "native/native.tests/testData/caches/testSplitScheme"
    }

    @BeforeEach
    fun assumeCachesAreEnabled() {
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
        Assumptions.assumeFalse(targets.testTarget == KonanTarget.MINGW_X64)
    }

    @Test
    @TestMetadata("simple")
    fun testSplitSchemeBuildsCaches() {
        val rootDir = ForTestCompileRuntime.transformTestDataPath("$TEST_SUITE_PATH/simple")
        val builtCachesDump = buildDir.resolve("built_caches_split.txt")

        val main = compileToExecutableInOneStage(
            rootDir.resolve("main"),
            tryPassSystemCacheDirectory = false,
            freeCompilerArgs = TestCompilerArgs(
                [
                    "-Xcompilation-scheme=split",
                    "-g",
                    "-Xdump-built-caches-to=${builtCachesDump.absolutePath}",
                ]
            ),
        ).executableFile

        assertTrue(main.exists())
        // Since the dump cache list exist, we can be sure that caches were built in split mode!
        assertTrue(
            builtCachesDump.exists(),
            "expected the split scheme to trigger cache building (no dump file at $builtCachesDump)"
        )
    }

    @Test
    @TestMetadata("simple")
    fun testClosedSchemeDoesNotBuildCaches() {
        val rootDir = ForTestCompileRuntime.transformTestDataPath("$TEST_SUITE_PATH/simple")
        val builtCachesDump = buildDir.resolve("built_caches_closed.txt")

        val main = compileToExecutableInOneStage(
            rootDir.resolve("main"),
            tryPassSystemCacheDirectory = false,
            freeCompilerArgs = TestCompilerArgs(
                [
                    "-g",
                    "-Xdump-built-caches-to=${builtCachesDump.absolutePath}",
                ]
            ),
        ).executableFile

        assertTrue(main.exists())
        assertFalse(
            builtCachesDump.exists(),
            "cache building must not be triggered without the split scheme"
        )
    }
}
