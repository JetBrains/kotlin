package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseDummyTestCaseGroupProvider
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.klib.runSanityTest
import org.jetbrains.kotlin.test.klib.runSanityTestNotMuted
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/"

@Tag("sanity")
@UseDummyTestCaseGroupProvider()
@TestMetadata(testDataRoot)
@Tag("aggregate-first-stage")
class CustomNativeCompilerFirstStageSanity : AbstractCustomNativeCompilerFirstStageTest() {
    @Test
    fun checkPassed() {
        runSanityTestNotMuted(testDataRoot + "green.kt")
    }

    @Test
    fun checkGreenNeedsUnmuting() {
        val exception = assertThrows<AssertionError> {
            runSanityTest(testDataRoot + "greenNeedsUnmuting.kt")
        }
        val expected = "Looks like this test can be unmuted. " +
                "Remove ${customNativeCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE directive"
        assertEquals(expected, exception.message)
    }

    @Test
    fun checkIncorrectBoxResult() {
        val exception = assertThrows<AssertionError> {
            runSanityTest(testDataRoot + "incorrectBoxResult.kt")
        }
        assertTrue(exception.message?.contains("Test failed with: FAIL. Expected <OK>, actual <FAIL>") == true, exception.message)
    }

    @Test
    fun checkMutedWithIgnoreRuntimeErrors1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runSanityTest(testDataRoot + "mutedWithIgnoreRuntimeErrors1stStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkNotMutedWithIgnoreRuntimeErrors2ndStage() {
        val exception = assertThrows<AssertionError> {
            runSanityTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        assertTrue(exception.message?.contains("Test failed with: FAIL. Expected <OK>, actual <FAIL>") == true, exception.message)
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runSanityTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkRecompilePassed() {
        // `// RECOMPILE` test directive is unknown to Native testinfra, so it must not affect test runs
        runSanityTestNotMuted(testDataRoot + "recompile.kt")
    }
}
