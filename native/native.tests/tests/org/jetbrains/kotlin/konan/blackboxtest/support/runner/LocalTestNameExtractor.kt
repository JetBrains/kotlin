/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.LoggedData
import org.jetbrains.kotlin.konan.blackboxtest.support.TestName
import org.jetbrains.kotlin.konan.blackboxtest.support.util.GTestListing
import org.jetbrains.kotlin.konan.blackboxtest.support.util.TestOutputFilter
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import kotlin.time.Duration

internal class LocalTestNameExtractor(
    override val executable: TestExecutable,
    executionTimeout: Duration
) : AbstractLocalProcessRunner<Collection<TestName>>(executionTimeout) {
    override val visibleProcessName get() = "Test name extractor"
    override val programArgs = listOf(executable.executableFile.path, "--ktest_list_tests")
    override val outputFilter get() = TestOutputFilter.NO_FILTERING

    override fun getLoggedParameters() = LoggedData.TestRunParameters(
        compilerCall = executable.loggedCompilerCall,
        testCaseId = null,
        runArgs = programArgs,
        runParameters = null
    )

    override fun buildResultHandler(runResult: RunResult.Completed) = ResultHandler(runResult)

    override fun handleUnexpectedFailure(t: Throwable) = fail {
        LoggedData.TestRunUnexpectedFailure(getLoggedParameters(), t)
            .withErrorMessage("Test name extraction failed with unexpected exception.")
    }

    inner class ResultHandler(
        runResult: RunResult.Completed
    ) : AbstractLocalProcessRunner<Collection<TestName>>.ResultHandler(runResult) {
        override fun getLoggedRun() = LoggedData.TestRun(getLoggedParameters(), runResult)
        override fun doHandle() = GTestListing.parse(runResult.processOutput.stdOut.filteredOutput)
    }
}
