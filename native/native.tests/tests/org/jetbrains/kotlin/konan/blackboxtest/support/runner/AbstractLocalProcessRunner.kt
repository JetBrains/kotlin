/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import kotlinx.coroutines.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.AbstractRunner.AbstractRun
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.UnfilteredProcessOutput.Companion.launchReader
import org.jetbrains.kotlin.konan.blackboxtest.support.util.TestOutputFilter
import java.io.ByteArrayOutputStream
import kotlin.time.*

internal abstract class AbstractLocalProcessRunner<R>(private val executionTimeout: Duration) : AbstractRunner<R>() {
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

            val process: Process
            val hasFinishedOnTime: Boolean

            val duration = measureTime {
                process = ProcessBuilder(programArgs).directory(executable.executableFile.parentFile).start()
                customizeProcess(process)

                unfilteredOutputReader = launchReader(unfilteredOutput, process)

                hasFinishedOnTime = process.waitFor(
                    executionTimeout.toLong(DurationUnit.MILLISECONDS),
                    DurationUnit.MILLISECONDS.toTimeUnit()
                )
            }

            if (hasFinishedOnTime) {
                unfilteredOutputReader.join() // Wait until all output streams are drained.

                RunResult.Completed(
                    exitCode = process.exitValue(),
                    duration = duration,
                    processOutput = unfilteredOutput.toProcessOutput(outputFilter)
                )
            } else {
                val exitCode: Int? = try { // It could happen just by an accident that the process has exited by itself.
                    val exitCode = process.exitValue() // Fetch exit code.
                    unfilteredOutputReader.join() // Wait until all streams are drained.
                    exitCode
                } catch (_: IllegalThreadStateException) { // Still not destroyed.
                    unfilteredOutputReader.cancel() // Cancel it. No need to read streams, actually.
                    process.destroyForcibly() // kill -9
                    null
                }

                RunResult.TimeoutExceeded(
                    timeout = executionTimeout,
                    exitCode = exitCode,
                    duration = duration,
                    processOutput = unfilteredOutput.toProcessOutput(outputFilter)
                )
            }
        }
    }

    abstract override fun buildResultHandler(runResult: RunResult.Completed): ResultHandler // Narrow returned type.

    abstract inner class ResultHandler(runResult: RunResult.Completed) : AbstractRunner<R>.ResultHandler(runResult) {
        override fun handle(): R {
            verifyExpectation(0, runResult.exitCode) { "$visibleProcessName exited with non-zero code." }

            return doHandle()
        }

        protected abstract fun doHandle(): R
    }
}

private class UnfilteredProcessOutput {
    private val stdOut = ByteArrayOutputStream()
    private val stdErr = ByteArrayOutputStream()

    fun toProcessOutput(outputFilter: TestOutputFilter): ProcessOutput = ProcessOutput(
        stdOut = outputFilter.filter(stdOut.toString(Charsets.UTF_8)),
        stdErr = stdErr.toString(Charsets.UTF_8)
    )

    companion object {
        fun CoroutineScope.launchReader(unfilteredOutput: UnfilteredProcessOutput, process: Process): Job = launch {
            launch { process.inputStream.copyTo(unfilteredOutput.stdOut) }
            launch { process.errorStream.copyTo(unfilteredOutput.stdErr) }
        }
    }
}
