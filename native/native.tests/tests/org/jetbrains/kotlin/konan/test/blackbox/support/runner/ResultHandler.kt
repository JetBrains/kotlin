/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.nio.charset.StandardCharsets

internal class ResultHandler(
    runResult: RunResult,
    private val checks: TestRunChecks,
    private val testRun: TestRun,
    private val loggedParameters: LoggedData.TestRunParameters,
) : AbstractResultHandler<Unit>(runResult) {

    override fun getLoggedRun() = LoggedData.TestRun(loggedParameters, runResult)

    override fun handle() {
        val failedResults = checks.map { check ->
            check.apply(testRun, runResult)
        }.filterIsInstance<TestRunCheck.Result.Failed>()
        if (!testRun.expectedFailure) {
            val exceptions = failedResults.map {
                AssertionFailedError(
                    it.reason,
                    it.expectedFile?.let { FileInfo(it.absolutePath, it.readText().convertLineSeparators().toByteArray(StandardCharsets.UTF_8)) },
                    it.actual,
                )
            }
            System.err.println(getLoggedRun())
            JUnit5Assertions.failAll(exceptions)
        } else {
            val runResultInfo = buildString {
                appendLine("TestCase Kind: ${testRun.testCase.kind}")
                appendLine("TestCaseId: ${testRun.testCase.id}")
                appendLine("Exit code: ${runResult.exitCode}")
                appendLine("Filtered test output is")
                appendLine(runResult.processOutput.stdOut.filteredOutput.let {
                    if (it.isNotEmpty()) ":\n$it" else " empty."
                })
                appendLine(runResult.processOutput.stdOut.testReport)
            }
            verifyExpectation(failedResults.isNotEmpty()) {
                "Test did not fail as expected: $runResultInfo"
            }
            println("Test failed as expected.\n$runResultInfo")
            if (failedResults.isNotEmpty()) {
                println("Diagnostics are:")
                failedResults.forEach(::println)
            }
        }
    }
}
