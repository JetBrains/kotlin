package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.ComparisonFailure
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("sanity")
class CustomJsCompilerSecondStageSanity : AbstractCustomJsCompilerSecondStageTest() {
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
                "Remove ${customJsCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE directive"
        assertEquals(expected, exception.message)
    }

    @Test
    fun checkIncorrectBoxResult() {
        val exception = assertThrows<ComparisonFailure> {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        }
        assertEquals("expected:<[OK]> but was:<[FAIL]>", exception.message)
    }

    @Test
    fun checkNotMutedWithIgnoreBackendErrors1stStage() {
        val exception = assertThrows<ComparisonFailure> {
            runTest(testDataRoot + "mutedWithIgnoreBackendErrors1stStage.kt")
        }
        assertEquals("expected:<[OK]> but was:<[FAIL]>", exception.message)
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
        Assumptions.assumeTrue(LanguageVersion.LATEST_STABLE == customJsCompilerSettings.defaultLanguageVersion)
        // current testdata is expected to be parsed by the current frontend. So errors must not be muted
        val exception = assertThrows<Exception> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertTrue(exception.message!!.startsWith("UNRESOLVED_REFERENCE: Unresolved reference 'FAIL'. at mutedDueToFrontendErrorWithCustom1stStage.kt:"),
            "Unexpected exception message: ${exception.message}")
    }

    @Test
    fun checkFailedDueToFrontendErrorWithCustom2ndStageOfOldLV() {
        Assumptions.assumeFalse(LanguageVersion.LATEST_STABLE == customJsCompilerSettings.defaultLanguageVersion)
        // Some tests cannot be compiled with previous LV, so the frontend errors must be muted
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertEquals(null, exception.message)
    }
}
