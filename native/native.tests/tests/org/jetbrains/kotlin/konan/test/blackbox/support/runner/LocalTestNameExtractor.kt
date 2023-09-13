/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.util.GTestListing
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail

internal class LocalTestNameExtractor(
    override val executable: TestExecutable,
    checks: TestRunChecks
) : AbstractLocalProcessRunner<Collection<TestName>>(checks) {
    override val visibleProcessName get() = "Test name extractor"
    override val programArgs = listOf(executable.executableFile.path, "--ktest_list_tests")
    override val outputFilter get() = TestOutputFilter.NO_FILTERING

    override fun getLoggedParameters() = LoggedData.TestRunParameters(
        compilationToolCall = executable.loggedCompilationToolCall,
        testCaseId = null,
        runArgs = programArgs,
        runParameters = null
    )

    override fun buildResultHandler(runResult: RunResult) =
        TestNameResultHandler(runResult, visibleProcessName, checks, getLoggedParameters())

    override fun handleUnexpectedFailure(t: Throwable) = fail {
        LoggedData.TestRunUnexpectedFailure(getLoggedParameters(), t)
            .withErrorMessage("Test name extraction failed with unexpected exception.")
    }
}

internal class TestNameResultHandler(
    runResult: RunResult,
    visibleProcessName: String,
    checks: TestRunChecks,
    private val loggedParameters: LoggedData.TestRunParameters
) : LocalResultHandler<Collection<TestName>>(runResult, visibleProcessName, checks) {
    override fun getLoggedRun() = LoggedData.TestRun(loggedParameters, runResult)
    override fun doHandle() = GTestListing.parse(runResult.processOutput.stdOut.filteredOutput)
}
