package org.jetbrains.kotlin.konan.test.klib

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("sanity")
@Tag("aggregate")
class CustomNativeCompilerFirstStageSanity : AbstractCustomNativeCompilerFirstStageTest() {
    private val testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/"

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
                "Remove ${customNativeCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE directive"
        assertEquals(expected, exception.message)
    }

    @Test
    fun checkIncorrectBoxResult() {
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        }
        assertTrue(exception.message?.contains("Test failed with: FAIL. Expected <OK>, actual <FAIL>") == true, exception.message)
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
        assertTrue(exception.message?.contains("Test failed with: FAIL. Expected <OK>, actual <FAIL>") == true, exception.message)
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkRecompilePassed() {
        // `// RECOMPILE` test directive is unknown to Native testinfra, so it must not affect test runs
        runTest(testDataRoot + "recompile.kt")
    }
}
