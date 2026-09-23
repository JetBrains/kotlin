/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.wasm.test.handlers.WasmVMException
import org.junit.jupiter.api.Assertions
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
        // The grouped launcher validates the box result itself, throwing a plain `AssertionError` rather than
        // `WasmVMException("Wrong box result")` — in the `kotlin.test.assertEquals` message format expected below.
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
        // The first-stage ignore directive isolates the test (see `WasmGroupingTestIsolator`), so it runs on the
        // box-export path, where the runner itself reports the wrong box result instead of the grouped launcher.
        val exception = assertThrows<WasmVMException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors1stStage.kt")
        }
        assertEquals("WasmVM V8 (dev) failed", exception.message)
        assertContains(exception.cause!!.message!!, """Wrong box result 'FAIL'; Expected "OK"""")
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

    private inline fun <reified T : Throwable> assertThrowsIfNotMuted(executable: () -> Unit): T {
        val throwable: Throwable? = try {
            executable()
        } catch (caught: Throwable) {
            caught
        } as? Throwable

        if (throwable is TestAbortedException) throw throwable

        return Assertions.assertThrows(T::class.java) {
            if (throwable != null) {
                throw throwable
            }
        }
    }
}
