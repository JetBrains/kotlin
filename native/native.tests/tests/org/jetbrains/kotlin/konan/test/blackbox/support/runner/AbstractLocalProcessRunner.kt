/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import com.intellij.openapi.util.text.StringUtilRt.convertLineSeparators
import kotlinx.coroutines.*
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.AbstractRunner.AbstractRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExecutionTimeout
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.UnfilteredProcessOutput.Companion.launchReader
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.junit.jupiter.api.Assertions.fail
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.*

internal abstract class AbstractLocalProcessRunner<R>(protected val checks: TestRunChecks) : AbstractRunner<R>() {
    protected abstract val visibleProcessName: String
    protected abstract val executable: TestExecutable
    protected abstract val programArgs: List<String>
    protected abstract val outputFilter: TestOutputFilter

    protected open fun customizeProcess(process: Process) = Unit

    @OptIn(ExperimentalTime::class)
    final override fun buildRun() = AbstractRun {
        runBlocking(Dispatchers.IO) {
            val unfilteredOutput = UnfilteredProcessOutput()
            val unfilteredOutputReader: Job

            val executionTimeout: Duration = checks.executionTimeoutCheck.timeout

            val process: Process
            val hasFinishedOnTime: Boolean

            // Don't ignore IO errors that happen just after the process is started.
            val ignoreIOErrorsInProcessOutput = AtomicBoolean(false)

            val duration = measureTime {
                process = ProcessBuilder(programArgs).directory(executable.executableFile.parentFile).start()
                customizeProcess(process)

                unfilteredOutputReader = launchReader(
                    unfilteredOutput,
                    processStdout = process.inputStream,
                    processStderr = process.errorStream,
                    ignoreIOErrorsInProcessOutput
                )

                hasFinishedOnTime = process.waitFor(
                    executionTimeout.toLong(DurationUnit.MILLISECONDS),
                    DurationUnit.MILLISECONDS.toTimeUnit()
                )
            }

            val exitCode: Int? = if (hasFinishedOnTime) {
                unfilteredOutputReader.join() // Wait until all output streams are drained.
                process.exitValue()
            } else {
                try { // It could happen just by an accident that the process has exited by itself.
                    val exitCode = process.exitValue() // Fetch exit code.
                    unfilteredOutputReader.join() // Wait until all streams are drained.
                    exitCode
                } catch (_: IllegalThreadStateException) { // Still not destroyed.
                    ignoreIOErrorsInProcessOutput.set(true) // Ignore IO errors caused by the closed streams of the killed process.
                    unfilteredOutputReader.cancel() // Cancel it. No need to read streams, actually.
                    process.destroyForcibly() // kill -9
                    null
                }
            }

            RunResult(
                exitCode = exitCode,
                timeout = executionTimeout,
                duration = duration,
                hasFinishedOnTime = hasFinishedOnTime,
                processOutput = unfilteredOutput.toProcessOutput(outputFilter)
            )
        }
    }

    abstract override fun buildResultHandler(runResult: RunResult): LocalResultHandler<R> // ?? Narrow returned type.
}

internal abstract class LocalResultHandler<R>(
    runResult: RunResult,
    private val visibleProcessName: String,
    private val checks: TestRunChecks
) : AbstractResultHandler<R>(runResult) {
    override fun handle(): R {
        checks.forEach { check ->
            when (check) {
                is ExecutionTimeout.ShouldNotExceed -> verifyExpectation(runResult.hasFinishedOnTime) {
                    "Timeout exceeded during test execution."
                }
                is ExecutionTimeout.ShouldExceed -> verifyExpectation(!runResult.hasFinishedOnTime) {
                    "Test is expected to fail with exceeded timeout, which hasn't happened."
                }
                is ExitCode -> {
                    // Don't check exit code if it is unknown.
                    val knownExitCode: Int = runResult.exitCode ?: return@forEach
                    when (check) {
                        is ExitCode.Expected -> verifyExpectation(knownExitCode == check.expectedExitCode) {
                            "$visibleProcessName exit code is $knownExitCode while ${check.expectedExitCode} was expected."
                        }
                        is ExitCode.AnyNonZero -> verifyExpectation(knownExitCode != 0) {
                            "$visibleProcessName exited with zero code, which wasn't expected."
                        }
                    }
                }
                is TestRunCheck.OutputDataFile -> {
                    val expectedOutput = check.file.readText()
                    val actualFilteredOutput = runResult.processOutput.stdOut.filteredOutput + runResult.processOutput.stdErr

                    // Don't use verifyExpectation(expected, actual) to avoid exposing potentially large test output in exception message
                    // and blowing up test logs.
                    verifyExpectation(convertLineSeparators(expectedOutput) == convertLineSeparators(actualFilteredOutput)) {
                        "Tested process output mismatch. See \"TEST STDOUT\" and \"EXPECTED OUTPUT DATA FILE\" below."
                    }
                }
                is TestRunCheck.OutputMatcher -> {
                    try {
                        verifyExpectation(check.match(runResult.processOutput.stdOut.filteredOutput)) {
                            "Tested process output has not passed validation."
                        }
                    } catch (t: Throwable) {
                        if (t is Exception || t is AssertionError) {
                            fail<Nothing>(
                                getLoggedRun().withErrorMessage("Tested process output has not passed validation: " + t.message),
                                t
                            )
                        } else {
                            throw t
                        }
                    }
                }
            }
        }

        return doHandle()
    }

    protected abstract fun doHandle(): R
}

private class UnfilteredProcessOutput {
    private val stdOut = ByteArrayOutputStream()
    private val stdErr = ByteArrayOutputStream()

    fun toProcessOutput(outputFilter: TestOutputFilter): ProcessOutput = ProcessOutput(
        stdOut = outputFilter.filter(stdOut.toString(Charsets.UTF_8.name())),
        stdErr = stdErr.toString(Charsets.UTF_8.name())
    )

    companion object {
        fun CoroutineScope.launchReader(
            unfilteredOutput: UnfilteredProcessOutput,
            processStdout: InputStream,
            processStderr: InputStream,
            ignoreIOErrors: AtomicBoolean
        ): Job = launch {
            fun InputStream.safeCopyTo(output: OutputStream) {
                try {
                    copyTo(output)
                } catch (e: IOException) {
                    if (ignoreIOErrors.get()) { // Note: Just checking `!process.isAlive` seems to be not reliable in concurrent environment.
                        // The IO exception might be caused by the closed stream due to process death. Just ignore.
                    } else {
                        // The process is still alive. Some I/O error happened, which is better to rethrow.
                        throw e
                    }
                }
            }

            launch { processStdout.safeCopyTo(unfilteredOutput.stdOut) }
            launch { processStderr.safeCopyTo(unfilteredOutput.stdErr) }
        }
    }
}
