/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.compileLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.toOutput
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * This test asserts that K/N compiler "agrees" to try to compile klib for any target
 * on any host (i.e. it doesn't emit an error, doesn't crash, etc.)
 *
 * It doesn't check whether the resulting klib is same across all hosts or even valid
 * (i.e. compiler can emit empty klib and the test will still pass)
 */
@TestDataPath("\$PROJECT_ROOT/native/native.tests/testData/klib/crossCompilationOutput")
abstract class KlibCrossCompilationOutputTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata("klibCrossCompilation")
    fun testKlibCrossCompilation() {
        Assumptions.assumeFalse(
            HostManager.hostIsMac,
            "This test targets cross-compilation to iOS and therefore disabled on Macs, as compiling iOS on Mac isn't interesting for this test"
        )
        val rootDir = File("native/native.tests/testData/klib/crossCompilationOutput/klibCrossCompilation")
        val compilationResult = compileLibrary(testRunSettings, rootDir.resolve("hello.kt"))
        val expectedOutput = rootDir.resolve("output.txt")

        KotlinTestUtils.assertEqualsToFile(expectedOutput, compilationResult.toOutput().sanitizeCrossCompilationOutput())
    }

    private fun String.sanitizeCrossCompilationOutput(): String = lines().map { line ->
        when {
            KLIB_RESOLVER_TARGET_DOESNT_MATCH_DIAGNOSTIC_REGEX.matches(line) -> {
                val match = KLIB_RESOLVER_TARGET_DOESNT_MATCH_DIAGNOSTIC_REGEX.matchEntire(line)!!
                val pathToDist = match.groups[1]!!.value
                line.replace(pathToDist, KONAN_HOME_STUB)
            }

            KLIB_RESOLVER_UNRESOLVED_LIBRARY_DIAGNOSTIC_REGEX.matches(line) -> {
                val match = KLIB_RESOLVER_UNRESOLVED_LIBRARY_DIAGNOSTIC_REGEX.matchEntire(line)!!
                val klibRoots = match.groups[2]!!.value
                line.replace(klibRoots, KLIB_ROOTS_STUB)
            }

            else -> line
        }
    }.joinToString(separator = "\n")

    companion object {
        /**
         * Capturing groups:
         * 1 - path to K/N Dist ($KONAN_HOME)
         * 2 - mismatched library name
         * 3 - expected target
         * 4 - actual targets
         */
        private val KLIB_RESOLVER_TARGET_DOESNT_MATCH_DIAGNOSTIC_REGEX = """
            warning: KLIB resolver: Skipping '(.*)/klib/common/(.*)'\. The target doesn't match. Expected '(.*)', found \[(.*)]\.
        """.trimIndent().toRegex()

        /**
         * Capturing groups
         * 1 - library name
         * 2 - klib roots
         */
        private val KLIB_RESOLVER_UNRESOLVED_LIBRARY_DIAGNOSTIC_REGEX = """
            error: KLIB resolver: Could not find "(.*)" in \[(.*)]
        """.trimIndent().toRegex()

        private const val KLIB_ROOTS_STUB = "<klib roots>"
        private const val KONAN_HOME_STUB = "\$KONAN_HOME"
    }
}

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.TEST_TARGET, "ios_arm64")
class ClassicFEKlibCrossCompilationOutputTest : KlibCrossCompilationOutputTest()

@FirPipeline
@Tag("frontend-fir")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.TEST_TARGET, "ios_arm64")
class FirKlibCrossCompilationOutputTest : KlibCrossCompilationOutputTest()
