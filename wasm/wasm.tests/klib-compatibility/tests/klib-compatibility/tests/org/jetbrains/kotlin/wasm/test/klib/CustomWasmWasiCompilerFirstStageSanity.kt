/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.js.test.klib.customWasmWasiCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.wasm.test.handlers.WasmVMException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertContains
import kotlin.test.assertEquals

@Tag("sanity")
@Tag("aggregate")
class CustomWasmWasiCompilerFirstStageSanity :
    AbstractCustomWasmWasiCompilerFirstStageTest(testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/") {

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
                "Remove ${customWasmWasiCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE directive"
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
        // WASI runs only dev mode with a single VM (NodeJs), so a single WasmVMException is thrown directly.
        assertEquals("WasmVM NodeJs failed", exception.message)
        exception.cause!!.message!!.let {
            // WASI helper uses single quotes: Expected 'OK'
            assertContains(it, "Wrong box result 'FAIL'; Expected 'OK'", message = it)
            assertContains(it, "$testName/dev")
        }
    }

    @Test
    fun checkMutedWithIgnoreRuntimeErrors1stStage() {
        // TODO KT-87378 Reconsider behavior of IGNORE_* directives, so no exception would be thrown here
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors1stStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkNotMutedWithIgnoreRuntimeErrors2ndStage() {
        val exception = assertThrows<WasmVMException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        checkIncorrectBoxResult(exception, "mutedWithIgnoreRuntimeErrors2ndStage")
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStage() {
        // TODO KT-87378 Reconsider behavior of IGNORE_* directives, so no exception would be thrown here
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
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
}
