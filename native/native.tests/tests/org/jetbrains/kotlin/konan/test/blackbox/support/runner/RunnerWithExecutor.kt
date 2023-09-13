/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.AbstractRunner.AbstractRun
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TCTestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.native.executors.ExecuteRequest
import org.jetbrains.kotlin.native.executors.Executor
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal class RunnerWithExecutor(
    private val executor: Executor,
    private val testRun: TestRun
) : AbstractRunner<Unit>() {
    private val executable get() = testRun.executable

    private val outputFilter: TestOutputFilter
        get() = if (testRun.runParameters.has<TestRunParameter.WithTCTestLogger>()) TCTestOutputFilter else TestOutputFilter.NO_FILTERING

    private val programArgs = mutableListOf<String>().apply {
        testRun.runParameters.forEach { it.applyTo(this) }
    }

    private fun inputStreamFromTestParameter(): InputStream =
        testRun.runParameters.firstIsInstanceOrNull<TestRunParameter.WithInputData>()
            ?.let {
                ByteArrayInputStream(it.inputDataFile.readBytes())
            } ?: ByteArrayInputStream(byteArrayOf())

    override fun buildRun() = AbstractRun {
        val stdin = inputStreamFromTestParameter()
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val request = ExecuteRequest(
            executableAbsolutePath = executable.executableFile.absolutePath,
            args = programArgs,
            stdin = stdin,
            stdout = stdout,
            stderr = stderr,
            timeout = testRun.checks.executionTimeoutCheck.timeout
        )
        val response = executor.execute(request)
        RunResult(
            exitCode = response.exitCode,
            timeout = request.timeout,
            duration = response.executionTime,
            hasFinishedOnTime = request.timeout >= response.executionTime,
            processOutput = ProcessOutput(
                stdOut = outputFilter.filter(stdout.toString("UTF-8")),
                stdErr = stderr.toString("UTF-8")
            )
        )
    }

    override fun buildResultHandler(runResult: RunResult): ResultHandler = ResultHandler(
        runResult = runResult,
        visibleProcessName = "Test process under Executor ${executor::class.simpleName}",
        checks = testRun.checks,
        testRun = testRun,
        loggedParameters = getLoggedParameters()
    )

    override fun getLoggedParameters() = LoggedData.TestRunParameters(
        compilationToolCall = executable.loggedCompilationToolCall,
        testCaseId = testRun.testCaseId,
        runArgs = programArgs,
        runParameters = testRun.runParameters
    )

    override fun handleUnexpectedFailure(t: Throwable) = JUnit5Assertions.fail {
        LoggedData.TestRunUnexpectedFailure(getLoggedParameters(), t)
            .withErrorMessage("Test execution failed with unexpected exception.")
    }
}