/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.runner

import org.jetbrains.kotlin.konan.blackboxtest.*
import org.jetbrains.kotlin.konan.blackboxtest.LoggedData
import org.jetbrains.kotlin.konan.blackboxtest.TestRun
import org.jetbrains.kotlin.konan.blackboxtest.TestRunParameter
import org.jetbrains.kotlin.konan.blackboxtest.get

internal class LocalTestRunner(private val testRun: TestRun) : AbstractLocalProcessRunner<Unit>() {
    override val visibleProcessName get() = "Tested process"
    override val executable get() = testRun.executable

    override val programArgs = buildList {
        add(executable.executableFile.path)
        testRun.runParameters.forEach { it.applyTo(this) }
    }

    private fun getLoggedParameters() = LoggedData.TestRunParameters(
        compilerCall = executable.loggedCompilerCall,
        origin = testRun.origin,
        runArgs = programArgs,
        runParameters = testRun.runParameters
    )

    override fun customizeProcess(process: Process) {
        testRun.runParameters.get<TestRunParameter.WithInputData> {
            process.outputStream.write(inputDataFile.readBytes())
            process.outputStream.flush()
        }
    }

    override fun buildResultHandler(runResult: RunResult) = ResultHandler(runResult)

    inner class ResultHandler(runResult: RunResult) : AbstractLocalProcessRunner<Unit>.ResultHandler(runResult) {
        override fun getLoggedRun() = LoggedData.TestRun(getLoggedParameters(), exitCode, stdOut, stdErr, durationMillis)

        override fun doHandle() {
            if (testRun.runParameters.has<TestRunParameter.WithGTestLogger>()) {
                verifyTestWithGTestRunner()
            } else {
                verifyPlainTest()
            }
        }

        private fun verifyTestWithGTestRunner() {
            val testStatuses = hashMapOf<TestStatus, MutableSet<TestName>>()
            val cleanStdOut = StringBuilder()

            var expectStatusLine = false
            stdOut.lines().forEach { line ->
                when {
                    expectStatusLine -> {
                        val matcher = GTEST_STATUS_LINE_REGEX.matchEntire(line)
                        if (matcher != null) {
                            // Read the line with test status.
                            val testStatus = matcher.groupValues[1]
                            val testName = matcher.groupValues[2]
                            testStatuses.getOrPut(testStatus) { hashSetOf() } += testName
                            expectStatusLine = false
                        } else {
                            // If current line is not a status line then it could be only the line with the process' output.
                            cleanStdOut.appendLine(line)
                        }
                    }
                    line.startsWith(GTEST_RUN_LINE_PREFIX) -> {
                        expectStatusLine = true // Next line contains either  test status.
                    }
                    else -> Unit
                }
            }

            verifyExpectation(testStatuses.isNotEmpty()) { "No tests have been executed." }

            val passedTests = testStatuses[GTEST_STATUS_OK]?.size ?: 0
            verifyExpectation(passedTests > 0) { "No passed tests." }

            testRun.runParameters.get<TestRunParameter.WithFilter> {
                val excessiveTests = testStatuses.getValue(GTEST_STATUS_OK).filter { testName -> !testMatches(testName) }
                verifyExpectation(excessiveTests.isEmpty()) { "Excessive tests have been executed: $excessiveTests." }
            }

            val failedTests = (testStatuses - GTEST_STATUS_OK).values.sumOf { it.size }
            verifyExpectation(0, failedTests) { "There are failed tests." }

            verifyOutputData(mergedOutput = cleanStdOut.toString() + stdErr)
        }

        private fun verifyPlainTest() = verifyOutputData(mergedOutput = stdOut + stdErr)

        private fun verifyOutputData(mergedOutput: String) {
            testRun.runParameters.get<TestRunParameter.WithExpectedOutputData> {
                verifyExpectation(expectedOutputDataFile.readText(), mergedOutput) {
                    "Tested process output mismatch. See \"TEST STDOUT\" and \"EXPECTED OUTPUT DATA FILE\" below."
                }
            }
        }
    }

    companion object {
        private const val GTEST_RUN_LINE_PREFIX = "[ RUN      ]"
        private val GTEST_STATUS_LINE_REGEX = Regex("^\\[\\s+([A-Z]+)\\s+]\\s+(\\S+)\\s+.*")
        private const val GTEST_STATUS_OK = "OK"
    }
}

private typealias TestStatus = String
private typealias TestName = String
