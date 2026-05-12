/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.wasm.test.handlers.WasmVMException
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
        val exception = assertThrows<WasmVMException> {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        }
        checkIncorrectBoxResult(exception, "incorrectBoxResult")
    }

    private fun checkIncorrectBoxResult(exception: WasmVMException, testName: String) {
        assertContains(exception.message!!, "WasmVM V8 failed", message = exception.message!!)
        exception.cause!!.message!!.let {
            assertContains(it, """Wrong box result 'FAIL'; Expected "OK"""", message = it)
        }
    }

    @Test
    fun checkNotMutedWithIgnoreRuntimeErrors1stStage() {
        val exception = assertThrows<WasmVMException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors1stStage.kt")
        }
        checkIncorrectBoxResult(exception, "mutedWithIgnoreRuntimeErrors1stStage")
    }

    @Test
    fun checkMutedWithIgnoreRuntimeErrors2ndStage() {
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
        val exception = assertThrows<Throwable> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        // Some tests cannot be compiled with previous LV. These are just ignored
        assertIs<TestAbortedException>(exception)
        assertEquals(null, exception.message)
    }

    @Test
    fun checkMutedWithWASM_IGNORE_FOR() {
        // `IGNORE_*` directives report failed test as ignored. Contrary to that, directive `// WASM_IGNORE_FOR: ...` reports test as passed,
        // since there are other executors that succeed, and it's an issue neither in the compiler nor in the test, but in the executor's mentioned in the directive.
        runTest(testDataRoot + "mutedWithWASM_IGNORE_FOR.kt")
    }

    @Test
    fun checkRecompilePasses() {
        runTest(testDataRoot + "recompile.kt")
    }
}
