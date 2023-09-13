/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TCTestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestReport
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.junit.jupiter.api.Assumptions.assumeFalse

internal class LocalTestRunner(private val testRun: TestRun) : AbstractLocalProcessRunner<Unit>(testRun.checks) {
    override val visibleProcessName get() = "Tested process"
    override val executable get() = testRun.executable

    override val programArgs = buildList {
        add(executable.executableFile.path)
        testRun.runParameters.forEach { it.applyTo(this) }
    }

    override val outputFilter: TestOutputFilter
        get() = if (testRun.runParameters.has<TestRunParameter.WithTCTestLogger>()) TCTestOutputFilter else TestOutputFilter.NO_FILTERING

    override fun getLoggedParameters() = LoggedData.TestRunParameters(
        compilationToolCall = executable.loggedCompilationToolCall,
        testCaseId = testRun.testCaseId,
        runArgs = programArgs,
        runParameters = testRun.runParameters
    )

    override fun customizeProcess(process: Process) {
        testRun.runParameters.get<TestRunParameter.WithInputData> {
            process.outputStream.write(inputDataFile.readBytes())
            process.outputStream.flush()
        }
    }

    override fun buildResultHandler(runResult: RunResult) = ResultHandler(
        runResult = runResult,
        visibleProcessName = visibleProcessName,
        checks = testRun.checks,
        testRun = testRun,
        loggedParameters = getLoggedParameters()
    )

    override fun handleUnexpectedFailure(t: Throwable) = fail {
        LoggedData.TestRunUnexpectedFailure(getLoggedParameters(), t)
            .withErrorMessage("Test execution failed with unexpected exception.")
    }
}

internal class ResultHandler(
    runResult: RunResult,
    visibleProcessName: String,
    checks: TestRunChecks,
    private val testRun: TestRun,
    private val loggedParameters: LoggedData.TestRunParameters
) : LocalResultHandler<Unit>(runResult, visibleProcessName, checks) {
    override fun getLoggedRun() = LoggedData.TestRun(loggedParameters, runResult)

    override fun doHandle() {
        verifyTestReport(runResult.processOutput.stdOut.testReport)
    }

    private fun verifyTestReport(testReport: TestReport?) {
        if (testReport == null) return

        verifyExpectation(!testReport.isEmpty()) { "No tests have been found." }

        testRun.runParameters.get<TestRunParameter.WithFilter> {
            verifyNoSuchTests(
                testReport.passedTests.filter { testName -> !testMatches(testName) },
                "Excessive tests have been executed"
            )

            verifyNoSuchTests(
                testReport.ignoredTests.filter { testName -> !testMatches(testName) },
                "Excessive tests have been ignored"
            )
        }

        verifyNoSuchTests(testReport.failedTests, "There are failed tests")

        assumeFalse(testReport.ignoredTests.isNotEmpty() && testReport.passedTests.isEmpty(), "Test case is disabled")
    }

    private fun verifyNoSuchTests(tests: Collection<TestName>, subject: String) = verifyExpectation(tests.isEmpty()) {
        buildString {
            append(subject).append(':')
            tests.forEach { appendLine().append(" - ").append(it) }
        }
    }
}
