/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestReport
import org.junit.jupiter.api.Assumptions.assumeFalse

internal class ResultHandler(
    runResult: RunResult,
    visibleProcessName: String,
    checks: TestRunChecks,
    private val testRun: TestRun,
    private val loggedParameters: LoggedData.TestRunParameters
) : LocalResultHandler<Unit>(runResult, visibleProcessName, checks, testRun.testCaseId, testRun.expectedFailure) {
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

        if (!testRun.expectedFailure)
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
