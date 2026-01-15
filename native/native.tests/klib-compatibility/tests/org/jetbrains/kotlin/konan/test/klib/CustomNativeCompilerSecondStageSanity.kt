package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("sanity")
class CustomNativeCompilerSecondStageSanity : AbstractCustomNativeCompilerSecondStageTest() {
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
                "Remove ${customNativeCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE directive"
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
    fun checkNotMutedWithIgnoreBackendErrors1stStage() {
        val exception = assertThrows<AssertionError> {
            runTest(testDataRoot + "mutedWithIgnoreBackendErrors1stStage.kt")
        }
        assertTrue(exception.message?.contains("Test failed with: FAIL. Expected <OK>, actual <FAIL>") == true, exception.message)
    }

    @Test
    fun checkMutedWithIgnoreRuntimeErrors2ndStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkFailedDueToFrontendErrorWithCustom2ndStageOfLatestLV() {
        Assumptions.assumeTrue(LanguageVersion.LATEST_STABLE == customNativeCompilerSettings.defaultLanguageVersion)
        // current testdata is expected to be parsed by the current frontend. So errors must not be muted
        val exception = assertThrows<Exception> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertTrue(exception.message!!.startsWith("UNRESOLVED_REFERENCE: Unresolved reference 'FAIL'. at mutedDueToFrontendErrorWithCustom1stStage.kt:"),
                   "Unexpected exception message: ${exception.message}")
    }

    @Test
    fun checkFailedDueToFrontendErrorWithCustom2ndStageOfOldLV() {
        Assumptions.assumeFalse(LanguageVersion.LATEST_STABLE == customNativeCompilerSettings.defaultLanguageVersion)
        // Some tests cannot be compiled with previous LV, so the frontend errors must be muted
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
