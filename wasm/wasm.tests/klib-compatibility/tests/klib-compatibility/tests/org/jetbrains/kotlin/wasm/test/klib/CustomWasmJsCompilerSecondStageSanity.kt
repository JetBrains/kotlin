/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertEquals

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
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        }
        checkIncorrectBoxResult(exception)
    }

    private fun checkIncorrectBoxResult(exception: AssertionError) {
        assertEquals(
            true,
            exception.message?.contains("""Wrong box result 'FAIL'; Expected "OK""""),
            exception.message
        )
    }

    @Test
    fun checkNotMutedWithIgnoreBackendErrors1stStage() {
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "mutedWithIgnoreBackendErrors1stStage.kt")
        }
        checkIncorrectBoxResult(exception)
    }

    @Test
    fun checkMutedWithIgnoreRuntimeErrors2ndStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkRecompilePasses() {
        runTest(testDataRoot + "recompile.kt")
    }
}
