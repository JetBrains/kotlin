package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.test.klib.runSanityTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertContains
import kotlin.test.assertEquals

@Tag("sanity")
@Tag("aggregate")
class CustomJsCompilerFirstStageSanity :
    AbstractCustomJsCompilerFirstStageTest(testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/") {

    @Test
    fun checkPassed() {
        runSanityTest(testDataRoot + "green.kt")
    }

    @Test
    fun checkGreenNeedsUnmuting() {
        val exception = assertThrows<AssertionError> {
            runSanityTest(testDataRoot + "greenNeedsUnmuting.kt")
        }
        val expected = "Looks like this test can be unmuted. " +
                "Remove ${customJsCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE directive"
        assertEquals(expected, exception.message)
    }

    @Test
    fun checkIncorrectBoxResult() {
        val exception = assertThrows<AssertionError> {
            runSanityTest(testDataRoot + "incorrectBoxResult.kt")
        }
        assertContains(exception.message!!, "Test failed with: FAIL. Expected <OK>, actual <FAIL>.")
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
        // The directive is a no-op for this runner, so the test is not isolated, and its `box()` is checked by the grouped pipeline.
        val exception = assertThrows<AssertionError> {
            runSanityTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        assertContains(exception.message!!, "Test failed with: FAIL. Expected <OK>, actual <FAIL>.")
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runSanityTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertEquals(null, exception.message)
    }

    @Test
    fun checkRecompileIgnored() {
        val exception = assertThrows<TestAbortedException> {
            runSanityTest(testDataRoot + "recompile.kt")
        }
        assertEquals(null, exception.message)
    }
}
