/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.LoggedData
import org.jetbrains.kotlin.konan.blackboxtest.support.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.TestFunction
import org.jetbrains.kotlin.konan.blackboxtest.support.util.parseGTestListing
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import kotlin.time.Duration

internal class LocalTestFunctionExtractor(
    override val executable: TestExecutable,
    executionTimeout: Duration
) : AbstractLocalProcessRunner<Collection<TestFunction>>(executionTimeout) {
    override val visibleProcessName get() = "Test function extractor"
    override val programArgs = listOf(executable.executableFile.path, "--ktest_list_tests")

    override fun getLoggedParameters() = LoggedData.TestRunParameters(
        compilerCall = executable.loggedCompilerCall,
        testCaseId = null,
        runArgs = programArgs,
        runParameters = null
    )

    override fun buildResultHandler(runResult: RunResult.Completed) = ResultHandler(runResult)

    override fun handleUnexpectedFailure(t: Throwable) = JUnit5Assertions.fail {
        LoggedData.TestRunUnexpectedFailure(getLoggedParameters(), t)
            .withErrorMessage("Test function extraction failed with unexpected exception.")
    }

    inner class ResultHandler(
        runResult: RunResult.Completed
    ) : AbstractLocalProcessRunner<Collection<TestFunction>>.ResultHandler(runResult) {
        override fun getLoggedRun() = LoggedData.TestRun(getLoggedParameters(), runResult)
        override fun doHandle() = parseGTestListing(runResult.output.stdOut)
    }
}
