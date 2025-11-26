package org.jetbrains.kotlin.js.test.klib

import org.junit.ComparisonFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.TestAbortedException
import kotlin.test.assertEquals

class CustomJsCompilerFirstStageSanity : AbstractCustomJsCompilerFirstStageTest() {
    private val testDataRoot = "compiler/testData/klib/klib-compatibility/sanity/"

    @Test
    fun checkPassed() {
        runTest(testDataRoot + "green.kt")
    }

    @Test
    fun checkIncorrectBoxResult() {
        val exception = assertThrows<ComparisonFailure> {
            runTest(testDataRoot + "incorrectBoxResult.kt")
        }
        assertEquals("expected:<[OK]> but was:<[FAIL]>", exception.message)
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
        val exception = assertThrows<ComparisonFailure> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors2ndStage.kt")
        }
        assertEquals("expected:<[OK]> but was:<[FAIL]>", exception.message)
    }

    @Test
    fun checkMutedDueToFrontendErrorWithCustom1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedDueToFrontendErrorWithCustom1stStage.kt")
        }
        assertEquals(null, exception.message)
    }
}
