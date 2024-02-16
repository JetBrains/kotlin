/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtilRt.convertLineSeparators
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExecutionTimeout
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.native.executors.RunProcessResult
import org.jetbrains.kotlin.native.executors.runProcess
import org.junit.jupiter.api.Assumptions
import java.io.File
import kotlin.time.Duration

private fun RunResult.processOutputAsString(output: TestRunCheck.Output) = when (output) {
    TestRunCheck.Output.STDOUT -> processOutput.stdOut.filteredOutput
    TestRunCheck.Output.STDERR -> processOutput.stdErr
    TestRunCheck.Output.ALL -> processOutput.stdOut.filteredOutput + processOutput.stdErr
}

internal class ResultHandler(
    runResult: RunResult,
    private val visibleProcessName: String,
    private val checks: TestRunChecks,
    private val testRun: TestRun,
    private val loggedParameters: LoggedData.TestRunParameters,
) : AbstractResultHandler<Unit>(runResult) {

    override fun getLoggedRun() = LoggedData.TestRun(loggedParameters, runResult)

    override fun handle() {
        val diagnostics = buildList<String> {
            checks.forEach { check ->
                when (check) {
                    is ExecutionTimeout.ShouldNotExceed -> if (!runResult.hasFinishedOnTime) add(
                        "Timeout exceeded during test execution."
                    )
                    is ExecutionTimeout.ShouldExceed -> if (runResult.hasFinishedOnTime) add(
                        "Test is expected to fail with exceeded timeout, which hasn't happened."
                    )
                    is ExitCode -> {
                        // Don't check exit code if it is unknown.
                        val knownExitCode: Int = runResult.exitCode ?: return@forEach
                        when (check) {
                            is ExitCode.Expected -> if (knownExitCode != check.expectedExitCode) add(
                                "$visibleProcessName exit code is $knownExitCode while ${check.expectedExitCode} was expected."
                            )
                            is ExitCode.AnyNonZero -> if (knownExitCode == 0) add(
                                "$visibleProcessName exited with zero code, which wasn't expected."
                            )
                        }
                    }
                    is TestRunCheck.OutputDataFile -> {
                        val expectedOutput = check.file.readText()
                        val actualFilteredOutput = runResult.processOutputAsString(check.output)

                        // Don't use verifyExpectation(expected, actual) to avoid exposing potentially large test output in exception message
                        // and blowing up test logs.
                        if (convertLineSeparators(expectedOutput) != convertLineSeparators(actualFilteredOutput)) add(
                            "Tested process output mismatch. See \"TEST STDOUT\" and \"EXPECTED OUTPUT DATA FILE\" below."
                        )
                    }
                    is TestRunCheck.OutputMatcher -> {
                        try {
                            if (!check.match(runResult.processOutputAsString(check.output))) add(
                                "Tested process output has not passed validation."
                            )
                        } catch (t: Throwable) {
                            if (t is Exception || t is AssertionError) {
                                add("Tested process output has not passed validation: " + t.message)
                            } else {
                                throw t
                            }
                        }
                    }
                    is TestRunCheck.FileCheckMatcher -> {
                        val fileCheckDump = runResult.testExecutable.executable.fileCheckDump!!
                        val result = doFileCheck(check, fileCheckDump)

                        if (!(result.stdout.isEmpty() && result.stderr.isEmpty())) {
                            val shortOutText = result.stdout.lines().take(100)
                            val shortErrText = result.stderr.lines().take(100)
                            add(
                                "FileCheck matching of ${fileCheckDump.absolutePath}\n" +
                                        "with '--check-prefixes ${check.prefixes}'\n" +
                                        "failed with result=$result:\n" +
                                        shortOutText.joinToString("\n") + "\n" +
                                        shortErrText.joinToString("\n")
                            )
                        }
                    }
                    is TestRunCheck.TestFiltering -> {
                        if (check.testOutputFilter != TestOutputFilter.NO_FILTERING) {
                            val testReport = runResult.processOutput.stdOut.testReport

                            if (testReport == null) {
                                add("TestRun has TestFiltering enabled, but test report is null")
                            }
                            checkNotNull(testReport)

                            if (testReport.isEmpty()) add("No tests have been found. Test report is empty")

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

                            verifyNoSuchTests(testReport.failedTests, "Failed tests found in the test report")

                            Assumptions.assumeFalse(
                                testReport.ignoredTests.isNotEmpty() && testReport.passedTests.isEmpty(),
                                "Test case is disabled"
                            )
                        }
                    }
                }
            }
        }
        if (!testRun.expectedFailure) {
            verifyExpectation(diagnostics.isEmpty()) {
                diagnostics.joinToString("\n")
            }
        } else {
            val runResultInfo = buildString {
                appendLine("TestCaseId: ${testRun.testCase.id}")
                appendLine("Exit code: ${runResult.exitCode}")
                appendLine("Filtered test output is")
                appendLine(runResult.processOutput.stdOut.filteredOutput.let {
                    if (it.isNotEmpty()) ":\n$it" else " empty."
                })
                appendLine(runResult.processOutput.stdOut.testReport)
            }
            verifyExpectation(diagnostics.isNotEmpty()) {
                "Test did not fail as expected: $runResultInfo"
            }
            println("Test failed as expected.\n$runResultInfo")
            if (diagnostics.isNotEmpty()) {
                println("Diagnostics are:")
                diagnostics.forEach(::println)
            }
        }
    }

    private fun MutableList<String>.verifyNoSuchTests(tests: Collection<TestName>, subject: String) {
        if (tests.isNotEmpty()) {
            add(
                buildString {
                    append(subject).append(':')
                    tests.forEach { appendLine().append(" - ").append(it) }
                }
            )
        }
    }
}

internal fun doFileCheck(check: TestRunCheck.FileCheckMatcher, fileCheckDump: File): RunProcessResult {
    val fileCheckExecutable = check.settings.configurables.absoluteLlvmHome + File.separator + "bin" + File.separator +
            if (SystemInfo.isWindows) "FileCheck.exe" else "FileCheck"
    require(File(fileCheckExecutable).exists()) {
        "$fileCheckExecutable does not exist. Make sure Distribution for `settings.configurables` " +
                "was created using `propertyOverrides` to specify development variant of LLVM instead of user variant."
    }
    return try {
        runProcess(
            fileCheckExecutable,
            check.testDataFile.absolutePath,
            "--input-file",
            fileCheckDump.absolutePath,
            "--check-prefixes", check.prefixes,
            "--allow-deprecated-dag-overlap" // TODO specify it via new test directive for `function_attributes_at_callsite.kt`
        )
    } catch (t: Throwable) {
        RunProcessResult(Duration.ZERO, "FileCheck utility failed:", t.toString())
    }
}
