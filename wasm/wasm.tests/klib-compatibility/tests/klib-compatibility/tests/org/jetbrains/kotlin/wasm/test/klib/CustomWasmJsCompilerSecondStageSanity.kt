/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Tag("sanity")
@Tag("aggregate")
class CustomWasmJsCompilerSecondStageSanity :
    AbstractCustomWasmJsCompilerSecondStageTest(testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/") {

    @Test
    fun checkPassed() {
        runTest(testDataRoot + "green.kt")
    }

    @Test
    fun checkGreenNeedsUnmuting() {
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "greenNeedsUnmuting.kt")
        }
        val expected = "Looks like this test can be unmuted. " +
                "Remove ${customWasmJsCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE directive"
        assertEquals(expected, exception.message)
    }

    @Test
    fun checkIncorrectBoxResult() {
        // In the grouped (two-stage) pipeline the box is validated by the per-test `@Test` launcher run via the
        // unit-test runner; an incorrect box result therefore surfaces as the `kotlin.test.assertEquals` failure
        // re-thrown as an `AssertionError` (rather than the standalone `WasmVMException("Wrong box result")`).
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        }
        checkIncorrectBoxResult(exception)
    }

    private fun checkIncorrectBoxResult(exception: AssertionError) {
        assertContains(exception.message!!, "Test failed with: FAIL. Expected <OK>, actual <FAIL>.", message = exception.message!!)
    }

    @Test
    fun checkNotMutedWithIgnoreRuntimeErrors1stStage() {
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors1stStage.kt")
        }
        checkIncorrectBoxResult(exception)
    }

    @Test
    fun checkMutedWithIgnoreRuntimeErrors2ndStage() {
        // TODO KT-87378 Reconsider behavior of IGNORE_* directives, so no exception would be thrown here
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStageOfLatestLV() {
        Assumptions.assumeTrue(LanguageVersion.LATEST_STABLE == customWasmJsCompilerSettings.defaultLanguageVersion)
        val exception = assertThrows<Throwable> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        // Frontend errors are not suppressed when testing within one major compiler version
        assertIs<IllegalStateException>(exception)
        assertContains(exception.message!!, "UNRESOLVED_REFERENCE: Unresolved reference 'FAIL'. at mutedDueToFrontendErrorWithCustom1stStage.kt:")
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStageOfOldLV() {
        Assumptions.assumeFalse(LanguageVersion.LATEST_STABLE == customWasmJsCompilerSettings.defaultLanguageVersion)
        // TODO KT-87378 Reconsider behavior of IGNORE_* directives, so no exception would be thrown here
        val exception = assertThrows<Throwable> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        // Some tests cannot be compiled with previous LV. These are just ignored
        assertIs<TestAbortedException>(exception)
        assertEquals(null, exception.message)
    }

    @Test
    fun checkMutedWithWASM_IGNORE_FOR() {
        runTest(testDataRoot + "mutedWithWASM_IGNORE_FOR.kt")
    }

    @Test
    fun checkRecompilePasses() {
        runTest(testDataRoot + "recompile.kt")
    }

    /**
     * Drives both compilation stages synchronously for a single test, mirroring what
     * [org.jetbrains.kotlin.test.grouping.CompilerTestGroupingTestEngine] does for a single-sized batch.
     *
     * Generated box/boxInline tests are executed by the grouping test engine via `initTestRunnerAndCreateModuleStructure`;
     * this helper is used by the sanity tests that need to assert synchronously on the outcome of a single test.
     */
    private fun runTest(@TestDataFile filePath: String) {
        initTestRunnerAndCreateModuleStructure(filePath)
        try {
            nonGroupingRunner.runTestPreprocessing()
            nonGroupingRunner.runSteps()

            // Report first-stage failures first (and throw on a real, non-suppressed failure). If the first stage
            // failed or was muted/ignored, the grouping (second) stage must be skipped, exactly like the grouping
            // test engine excludes such tests from the batch. Otherwise both stages would contribute failures and
            // they'd be aggregated into a `MultipleFailuresError` instead of the single expected exception.
            val hadIgnoredFailuresOnFirstStage = nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
            if (hadIgnoredFailuresOnFirstStage) return

            val nonGroupingStageOutput = NonGroupingStageOutput(
                testServices = nonGroupingRunner.testServices,
                catchingExecutor = { wrapper, block ->
                    nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
                },
            )
            groupingStageRunner.run(listOf(nonGroupingStageOutput))

            // Exceptions from grouped facades were reported to the grouping runner's failures interceptor,
            // but failure suppressors must be run from the non-grouping runner, as they need access to the
            // real module structure of the specific test to extract directives from there.
            nonGroupingRunner.failuresInterceptor += groupingStageRunner.failuresInterceptor
            nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
        } finally {
            nonGroupingRunner.finalizeAndDispose()
        }
    }
}
