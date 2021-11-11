/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import kotlin.properties.Delegates

internal fun TestRun.runAndVerify() {
    val programArgs = mutableListOf<String>(executable.executableFile.path)
    runParameters.forEach { it.applyTo(programArgs) }

    val loggedParameters = LoggedData.TestRunParameters(executable.loggedCompilerCall, executable.origin, programArgs, runParameters)

    val startTimeMillis = System.currentTimeMillis()
    val process = ProcessBuilder(programArgs).directory(executable.executableFile.parentFile).start()
    runParameters.get<TestRunParameter.WithInputData> {
        process.outputStream.write(inputDataFile.readBytes())
        process.outputStream.flush()
    }

    TestOutput(runParameters, process, startTimeMillis, loggedParameters).verify()
}

private class TestOutput(
    private val runParameters: List<TestRunParameter>,
    private val process: Process,
    private val startTimeMillis: Long,
    private val loggedParameters: LoggedData.TestRunParameters
) {
    private var exitCode: Int by Delegates.notNull()
    private lateinit var stdOut: String
    private lateinit var stdErr: String
    private var finishTimeMillis by Delegates.notNull<Long>()

    fun verify() {
        waitUntilExecutionFinished()

        verifyExpectation(0, exitCode) { "Tested process exited with non-zero code." }

        if (runParameters.has<TestRunParameter.WithGTestLogger>()) {
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

        verifyExpectation(true, testStatuses.isNotEmpty()) { "No tests have been executed." }

        val passedTests = testStatuses[GTEST_STATUS_OK]?.size ?: 0
        verifyExpectation(true, passedTests > 0) { "No passed tests." }

        runParameters.get<TestRunParameter.WithPackageName> {
            val excessiveTests = testStatuses.getValue(GTEST_STATUS_OK).filter { testName -> !testName.startsWith(packageName) }
            verifyExpectation(true, excessiveTests.isEmpty()) { "Excessive tests have been executed: $excessiveTests." }
        }

        val failedTests = (testStatuses - GTEST_STATUS_OK).values.sumOf { it.size }
        verifyExpectation(0, failedTests) { "There are failed tests." }

        runParameters.get<TestRunParameter.WithExpectedOutputData> {
            val mergedOutput = cleanStdOut.toString() + stdErr
            verifyExpectation(expectedOutputDataFile.readText(), mergedOutput) { "Tested process output mismatch." }
        }
    }

    private fun verifyPlainTest() {
        runParameters.get<TestRunParameter.WithExpectedOutputData> {
            val mergedOutput = stdOut + stdErr
            verifyExpectation(expectedOutputDataFile.readText(), mergedOutput) { "Tested process output mismatch." }
        }
    }

    private fun waitUntilExecutionFinished() {
        exitCode = process.waitFor()
        finishTimeMillis = System.currentTimeMillis()
        stdOut = process.inputStream.bufferedReader().readText()
        stdErr = process.errorStream.bufferedReader().readText()
    }

    private inline fun <T> verifyExpectation(expected: T, actual: T, crossinline errorMessageHeader: () -> String) {
        assertEquals(expected, actual) {
            val loggedTestRun = LoggedData.TestRun(loggedParameters, exitCode, stdOut, stdErr, finishTimeMillis - startTimeMillis)
            "${errorMessageHeader()}\n\n$loggedTestRun"
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
