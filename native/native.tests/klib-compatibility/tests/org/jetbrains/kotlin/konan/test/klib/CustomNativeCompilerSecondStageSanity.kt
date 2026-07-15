package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseDummyTestCaseGroupProvider
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.opentest4j.TestAbortedException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/"

@Tag("sanity")
@UseDummyTestCaseGroupProvider()
@TestMetadata(testDataRoot)
@Tag("aggregate-second-stage")
class CustomNativeCompilerSecondStageSanity : AbstractCustomNativeCompilerSecondStageTest() {
    @Test
    fun checkPassed() {
        runTest(testDataRoot + "green.kt")
    }

    @Test
    fun checkGreenNeedsUnmuting() {
        val exception = assertThrowsIfNotMuted<AssertionError> {
            runTest(testDataRoot + "greenNeedsUnmuting.kt")
        }
        val expected = "Looks like this test can be unmuted. " +
                "Remove ${customNativeCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_FRONTEND_ERRORS_WITH_CUSTOM_SECOND_STAGE directive"
        assertEquals(expected, exception.message)
    }

    @Test
    fun checkIncorrectBoxResult() {
        val exception = assertThrowsIfNotMuted<AssertionError> {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        }
        assertTrue(exception.message?.contains("Test failed with: FAIL. Expected <OK>, actual <FAIL>") == true, exception.message)
    }

    @Test
    fun checkNotMutedWithIgnoreRuntimeErrors1stStage() {
        val exception = assertThrowsIfNotMuted<AssertionError> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors1stStage.kt")
        }
        assertTrue(exception.message?.contains("Test failed with: FAIL. Expected <OK>, actual <FAIL>") == true, exception.message)
    }

    @Test
    fun checkMutedWithIgnoreRuntimeErrors2ndStage() {
        val exception = assertThrowsIfNotMuted<TestAbortedException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkFailedDueToFrontendErrorWithCustom2ndStageOfLatestLV() {
        Assumptions.assumeTrue(LanguageVersion.LATEST_STABLE == customNativeCompilerSettings.defaultLanguageVersion)
        // current testdata is expected to be parsed by the current frontend. So errors must not be muted
        val exception = assertThrowsIfNotMuted<Exception> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertTrue(exception.message!!.startsWith("UNRESOLVED_REFERENCE: Unresolved reference 'FAIL'. at mutedDueToFrontendErrorWithCustom1stStage.kt:"),
                   "Unexpected exception message: ${exception.message}")
    }

    @Test
    fun checkFailedDueToFrontendErrorWithCustom2ndStageOfOldLV() {
        Assumptions.assumeFalse(LanguageVersion.LATEST_STABLE == customNativeCompilerSettings.defaultLanguageVersion)
        // Some tests cannot be compiled with previous LV, so the frontend errors must be muted
        val exception = assertThrowsIfNotMuted<TestAbortedException> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkRecompilePassed() {
        // `// RECOMPILE` test directive is unknown to Native testinfra, so it must not affect test runs
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
