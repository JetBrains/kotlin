/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import com.intellij.openapi.util.text.StringUtilRt.convertLineSeparators
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.parseGTestReport
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import kotlin.time.Duration

internal class LocalTestRunner(
    private val testRun: TestRun,
    executionTimeout: Duration
) : AbstractLocalProcessRunner<Unit>(executionTimeout) {
    override val visibleProcessName get() = "Tested process"
    override val executable get() = testRun.executable

    override val programArgs = buildList {
        add(executable.executableFile.path)
        testRun.runParameters.forEach { it.applyTo(this) }
    }

    override fun getLoggedParameters() = LoggedData.TestRunParameters(
        compilerCall = executable.loggedCompilerCall,
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

    override fun buildResultHandler(runResult: RunResult.Completed) = ResultHandler(runResult)

    override fun handleUnexpectedFailure(t: Throwable) = fail {
        LoggedData.TestRunUnexpectedFailure(getLoggedParameters(), t)
            .withErrorMessageHeader("Test execution failed with unexpected exception.")
    }

    inner class ResultHandler(runResult: RunResult.Completed) : AbstractLocalProcessRunner<Unit>.ResultHandler(runResult) {
        override fun getLoggedRun() = LoggedData.TestRun(getLoggedParameters(), runResult)

        override fun doHandle() {
            if (testRun.runParameters.has<TestRunParameter.WithGTestLogger>()) {
                verifyTestWithGTestRunner()
            } else {
                verifyPlainTest()
            }
        }

        private fun verifyTestWithGTestRunner() {
            val testReport = parseGTestReport(runResult.output.stdOut)

            verifyExpectation(!testReport.isEmpty()) { "No tests have been executed." }

            val passedTests = testReport.getPassedTests()
            verifyExpectation(passedTests.isNotEmpty()) { "No passed tests." }

            testRun.runParameters.get<TestRunParameter.WithFilter> {
                val excessiveTests = passedTests.filter { testName -> !testMatches(testName) }
                verifyExpectation(excessiveTests.isEmpty()) { "Excessive tests have been executed: $excessiveTests." }
            }

            val failedTests = testReport.getFailedTests().size
            verifyExpectation(0, failedTests) { "There are $failedTests failed tests." }

            verifyOutputData(mergedOutput = testReport.cleanStdOut + runResult.output.stdErr)
        }

        private fun verifyPlainTest() = verifyOutputData(mergedOutput = runResult.output.stdOut + runResult.output.stdErr)

        private fun verifyOutputData(mergedOutput: String) {
            testRun.runParameters.get<TestRunParameter.WithExpectedOutputData> {
                verifyExpectation(convertLineSeparators(expectedOutputDataFile.readText()), convertLineSeparators(mergedOutput)) {
                    "Tested process output mismatch. See \"TEST STDOUT\" and \"EXPECTED OUTPUT DATA FILE\" below."
                }
            }
        }
    }
}
