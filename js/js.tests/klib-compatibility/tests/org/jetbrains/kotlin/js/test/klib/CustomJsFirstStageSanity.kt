package org.jetbrains.kotlin.js.test.klib

import org.junit.ComparisonFailure
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertEquals

@Tag("sanity")
class CustomJsCompilerFirstStageSanity :
    AbstractCustomJsCompilerFirstStageTest(testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/") {

    @Test
    fun checkPassed() {
        runTest(testDataRoot + "green.kt")
    }

    @Test
    fun checkGreenNeedsUnmuting() {
//        val exception = assertThrows<AssertionError> {
        try {
            runTest(testDataRoot + "greenNeedsUnmuting.kt")
        } catch (exception: Exception) {
            assertEquals("expected", exception::class.simpleName +"/"+ exception.message)
        }
//        val expected = "Looks like this test can be unmuted. " +
//                "Remove ${customJsCompilerSettings.defaultLanguageVersion} from the IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE directive"
//        assertEquals(expected, exception.message)
    }

    @Test
    fun checkIncorrectBoxResult() {
//        val exception = assertThrows<ComparisonFailure> {
        try {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        } catch (exception: Exception) {
            assertEquals("expected", exception::class.simpleName +"/"+ exception.message)
        }
//        assertEquals("expected:<[OK]> but was:<[FAIL]>", exception.message)
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
//        val exception = assertThrows<ComparisonFailure> {
        try {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        } catch (exception: Exception) {
            assertEquals("expected", exception::class.simpleName +"/"+ exception.message)
        }
//        assertEquals("expected:<[OK]> but was:<[FAIL]>", exception.message)
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertEquals(null, exception.message)
    }
}
