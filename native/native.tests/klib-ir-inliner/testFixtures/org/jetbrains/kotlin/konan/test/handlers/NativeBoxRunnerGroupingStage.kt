/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunParameter
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.*
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunners.createProperTestRunner
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TCTestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.native.executors.Executor
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.AbstractLauncherAdditionalSourceProvider.Companion.LAUNCHER_FACADE_CLASS_NAME_PATTERN
import org.jetbrains.kotlin.test.testInfraError
import java.io.File
import kotlin.test.assertIs

class NativeBoxRunnerGroupingStage(testServices: TestServices) : GroupingStageHandler<BinaryArtifacts.Native>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false
) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Native>
        get() = ArtifactKinds.Native

    override fun processArtifact(artifact: BinaryArtifacts.Native) {
        val testRun = createTestRun(artifact.executable, testServices)
        val testRunner = createProperTestRunner(testRun, testServices.testRunSettings) { executor, testRun ->
            RunnerWithExecutorAndPrettyHandler(executor, testRun, testServices) { e -> WrappedException.FromGroupingHandler(e, this) }
        }
        testRunner.run()
    }
}

private fun createTestRun(executable: File, testServices: TestServices): TestRun {
    val testKind = testServices.testRunSettings.testKind(testServices.moduleStructure.modules.firstOrNull()?.directives)
    val checks = TestRunChecks(
        executionTimeoutCheck = TestRunCheck.ExecutionTimeout.ShouldNotExceed(testServices.testRunSettings.get<Timeouts>().executionTimeout),
        testFiltering = TestRunCheck.TestFiltering(
            if (testKind in listOf(TestKind.REGULAR, TestKind.STANDALONE)) TCTestOutputFilter
            else TestOutputFilter.NO_FILTERING
        ),
        exitCodeCheck = TestRunCheck.ExitCode.Expected(0),
        outputDataFile = null,
        outputMatcher = null,
        fileCheckMatcher = null,
    )
    val success = TestCompilationResult.Success(
        TestCompilationArtifact.Executable(
            executableFile = executable,
            fileCheckStage = null,
            hasSyntheticAccessorsDump = false,
        ),
        LoggedData.dummyCompilerCall, // TODO: populate at least real compiler invocation arguments
    )
    val runParameters = getTestRunParameters(testKind, checks)
    val testRun = TestRun(
        displayName = /* Unimportant. Used only in dynamic tests. */ "",
        executable = TestExecutable.fromCompilationResult(testKind, success),
        runParameters = runParameters,
        testCase = TestCase(
            id = TestCaseId.Named("batch"),
            kind = TestKind.STANDALONE,
            modules = emptySet(),
            freeCompilerArgs = TestCompilerArgs(),
            nominalPackageName = PackageName.EMPTY,
            checks = checks,
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT),
            fileCheckStage = null,
            expectedFailure = false,
        ),
        checks = checks,
        expectedFailure = false,
    )
    return testRun
}

private fun getTestRunParameters(testKind: TestKind, checks: TestRunChecks): List<TestRunParameter> = when (testKind) {
    TestKind.STANDALONE, TestKind.REGULAR -> {
        // WithTCLogger relies on TCTestOutputFilter to be present in the checkers.
        assertIs<TCTestOutputFilter>(checks.testFiltering.testOutputFilter)
        listOf(TestRunParameter.WithTCTestLogger)
    }
    // Test-infrastructure invariant violation (not a failure of the code under test): throw a
    // TestInfrastructureException so it is never masked by failure suppressors (e.g. an IGNORE_BACKEND directive).
    else -> testInfraError("Not yet supported test kind: $testKind")
}

class RunnerWithExecutorAndPrettyHandler(
    executor: Executor,
    testRun: TestRun,
    val testServices: TestServices,
    val exceptionWrapper: (Throwable) -> WrappedException.FromGroupingHandler,
) : RunnerWithExecutor(executor, testRun) {
    override fun buildResultHandler(runResult: RunResult): PrettyResultsHandler {
        return PrettyResultsHandler(
            runResult = runResult,
            checks = testRun.checks,
            testRun = testRun,
            loggedParameters = getLoggedParameters(),
            testServices,
            exceptionWrapper,
        )
    }
}

class PrettyResultsHandler(
    runResult: RunResult,
    checks: TestRunChecks,
    testRun: TestRun,
    loggedParameters: LoggedData.TestRunParameters,
    val testServices: TestServices,
    val exceptionWrapper: (Throwable) -> WrappedException.FromGroupingHandler,
) : ResultHandler(runResult, checks, testRun, loggedParameters) {
    companion object {
        // failedResults reasons for failed test contain test name in the format:
        //   in isolated mode: `__launcher__Kt.runTest`
        //   in grouped mode: `<long_test_name>.__launcher_<hash>__Kt.runTest`,
        //     since the launcher file is named after the hash of the module name (see `AbstractLauncherAdditionalSourceProvider`)
        private val failedRegexWithTCLogger = """-\s+(.*)$LAUNCHER_FACADE_CLASS_NAME_PATTERN\.runTest""".toRegex()

        /** The packages of the failed tests that the reason of a failed check of a batch lists. */
        fun findFailedTestsWithTCLogger(failureReason: String): List<String> {
            return failedRegexWithTCLogger.findAll(failureReason)
                .map { it.groupValues }.distinct()
                .map { it[1].removeSuffix(".") }.toList()
        }
    }

    override fun handle() {
        super.handle()
        // Sanity check: make sure that every expected testcase was actually executed by the test executable.
        // Without this check a missing testcase (e.g. silently dropped from the batch) would go unnoticed
        // as long as the executable reports no failures.
        verifyAllTestCasesWereExecuted()
    }

    private fun verifyAllTestCasesWereExecuted() {
        // The test report is only available when the output is parsed by the TC test output filter.
        // For other filters there is no reliable way to enumerate the executed testcases, so skip the check.
        val testReport = runResult.processOutput.stdOut.testReport ?: return
        val executedTests = testReport.tests.reportedIds
        val phaseInputs = testServices.groupingStageInputs

        if (phaseInputs.size == 1) {
            // A single (isolated) testcase is not moved into a dedicated package, so it can't be matched by package name.
            // Verify that exactly one outcome was reported, counting duplicate reports for the same test name.
            val reportedOutcomeCount = testReport.reportedOutcomeCount
            checkTestInfrastructure(reportedOutcomeCount == 1) {
                "Expected exactly one executed testcase in the batch mode, but $reportedOutcomeCount outcomes were " +
                        "reported for $executedTests"
            }
            return
        }

        // Each grouped testcase is moved into a dedicated package computed by `BatchingPackageInserter.computePackage`,
        // and its launcher `runTest` function ends up in that very package. Therefore an expected testcase is considered
        // executed if there is an executed test whose package name matches the computed package.
        val executedPackages = executedTests.mapTo(mutableSetOf()) { it.packageName.toString() }
        val notExecutedTestCases = phaseInputs.filter { input ->
            BatchingPackageInserter.computePackage(input.testInfo) !in executedPackages
        }
        checkTestInfrastructure(notExecutedTestCases.isEmpty()) {
            "Not all expected testcases were executed. " +
                    "Expected ${phaseInputs.size} testcase(s), but only ${phaseInputs.size - notExecutedTestCases.size} of them were actually executed. " +
                    "The following testcase(s) were not executed: ${notExecutedTestCases.map { it.testInfo }}"
        }
    }

    override fun processNonExpectedFailure(failedResults: List<TestRunCheck.Result.Failed>) {
        val output = getLoggedRun().toString()
        // The batch always runs with the TeamCity logger (see `getTestRunParameters`).
        val failedTests = failedResults.flatMap { findFailedTestsWithTCLogger(it.reason) }
        val phaseInputs = testServices.groupingStageInputs

        checkTestInfrastructure(failedResults.isEmpty() || failedTests.isNotEmpty()) {
            "There should be at least one failed test, but none detected:\n" +
                    failedResults.joinToString("\n") + "\n\n" + output
        }
        if (phaseInputs.size == 1) {
            // This is a test-infrastructure invariant violation, not a failure of the code under test. Throw a
            // TestInfrastructureException so it is never masked by failure suppressors (e.g. an IGNORE_BACKEND directive).
            checkTestInfrastructure(failedTests.size <= 1) {
                "There should be at most one failed test in the batch mode, but there were $failedTests"
            }
            if (failedTests.isNotEmpty()) {
                phaseInputs.single().catchingExecutor.executeWithCatching(exceptionWrapper) {
                    super.processNonExpectedFailure(failedResults)
                }
            }
            return
        }

        for (failedTest in failedTests) {
            val correspondingInput = phaseInputs.find {
                val testInfo = it.testInfo
                val correspondingTestName = BatchingPackageInserter.computePackage(testInfo)
                correspondingTestName == failedTest
            } ?: testInfraError("Can't find corresponding input for $failedTest")
            correspondingInput.catchingExecutor.executeWithCatching(exceptionWrapper) {
                super.processNonExpectedFailure(failedResults)
            }
        }
    }
}
