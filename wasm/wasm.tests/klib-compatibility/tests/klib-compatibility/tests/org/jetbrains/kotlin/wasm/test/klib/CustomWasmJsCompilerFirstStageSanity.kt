/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.wasm.test.handlers.WasmVMException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@Tag("sanity")
@Tag("aggregate")
class CustomWasmJsCompilerFirstStageSanity :
    AbstractCustomWasmJsCompilerFirstStageTest(testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/") {

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
                "Remove ${customWasmJsCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE directive"
        assertEquals(expected, exception.message)
    }

    @Test
    fun checkIncorrectBoxResult() {
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        }
        checkIncorrectBoxResult(exception, "incorrectBoxResult")
    }

    private fun checkIncorrectBoxResult(exception: AssertionError, testName: String) {
        // Separate exceptions are raised for DEV and DCE builds.
        assertEquals("""Multiple Failures (2 failures)
	org.jetbrains.kotlin.wasm.test.handlers.WasmVMException: WasmVM V8 failed
	org.jetbrains.kotlin.wasm.test.handlers.WasmVMException: WasmVM V8 failed""", exception.message)
        assertEquals(2, exception.suppressedExceptions.size)
        for (exception in exception.suppressedExceptions) {
            assertIs<WasmVMException>(exception)
            assertEquals("WasmVM V8 failed", exception.message!!)
            exception.cause!!.message!!.let {
                assertContains(it, """Wrong box result 'FAIL'; Expected "OK"""", message = it)
            }
        }
        assertContains(exception.suppressedExceptions[0].cause!!.message!!, "$testName/dev")
        assertContains(exception.suppressedExceptions[1].cause!!.message!!, "$testName/dce")
    }

    @Test
    fun checkMutedWithIgnoreBackendErrors1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedWithIgnoreBackendErrors1stStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkNotMutedWithIgnoreRuntimeErrors2ndStage() {
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        checkIncorrectBoxResult(exception, "mutedWithIgnoreRuntimeErrors2ndStage")
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
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
