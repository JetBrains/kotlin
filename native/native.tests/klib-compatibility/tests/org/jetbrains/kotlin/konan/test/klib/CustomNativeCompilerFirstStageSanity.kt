package org.jetbrains.kotlin.konan.test.klib

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UseDummyTestCaseGroupProvider
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.TestMetadata
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
    fun checkMutedWithIgnoreRuntimeErrors1stStage() {
        val exception = assertThrows<TestAbortedException> {
            runTest(testDataRoot + "mutedWithIgnoreRuntimeErrors1stStage.kt")
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

    /**
     * Drives both compilation stages synchronously for a single test, mirroring what
     * [CompilerTestGroupingTestEngine] does for a single-sized batch.
     *
     * Generated box/boxInline tests are executed by the grouping test engine via `initTestRunnerAndCreateModuleStructure`;
     * this helper is used by the sanity tests that need to assert synchronously on the outcome of a single test.
     */
    private fun runTest(@TestDataFile filePath: String) {
        initTestRunnerAndCreateModuleStructure(filePath)
        try {
            nonGroupingRunner.runTestPreprocessing()
            nonGroupingRunner.runSteps()

            // Report first-stage failures first (and throw on a real, non-suppressed failure). If the first stage
            // failed or was muted/ignored, the grouping (second) stage must be skipped, exactly like the grouping
            // test engine excludes such tests from the batch. Otherwise both stages would contribute failures and
            // they'd be aggregated into a `MultipleFailuresError` instead of the single expected exception.
            val hadIgnoredFailuresOnFirstStage = nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
            if (hadIgnoredFailuresOnFirstStage) return

            val nonGroupingStageOutput = NonGroupingStageOutput(
                testServices = nonGroupingRunner.testServices,
                catchingExecutor = { wrapper, block ->
                    nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
                },
            )
            groupingStageRunner.run(listOf(nonGroupingStageOutput))

            // Exceptions from grouped facades were reported to the grouping runner's failures interceptor,
            // but failure suppressors must be run from the non-grouping runner, as they need access to the
            // real module structure of the specific test to extract directives from there.
            nonGroupingRunner.failuresInterceptor += groupingStageRunner.failuresInterceptor
            nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
        } finally {
            nonGroupingRunner.finalizeAndDispose()
        }
    }
}
