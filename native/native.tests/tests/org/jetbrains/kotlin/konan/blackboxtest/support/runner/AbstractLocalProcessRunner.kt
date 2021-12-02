/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.AbstractRunner.AbstractRun
import org.jetbrains.kotlin.konan.blackboxtest.support.util.TestOutputFilter
import org.jetbrains.kotlin.konan.blackboxtest.support.util.readOutput
import kotlin.time.*

internal abstract class AbstractLocalProcessRunner<R>(private val executionTimeout: Duration) : AbstractRunner<R>() {
    protected abstract val visibleProcessName: String
    protected abstract val executable: TestExecutable
    protected abstract val programArgs: List<String>
    protected abstract val outputFilter: TestOutputFilter

    protected open fun customizeProcess(process: Process) = Unit

    @OptIn(ExperimentalTime::class)
    final override fun buildRun() = AbstractRun {
        val (result, duration) = measureTimedValue {
            val process = ProcessBuilder(programArgs).directory(executable.executableFile.parentFile).start()
            customizeProcess(process)

            val hasFinishedOnTime = process.waitFor(
                executionTimeout.toLong(DurationUnit.MILLISECONDS),
                DurationUnit.MILLISECONDS.toTimeUnit()
            )

            process to hasFinishedOnTime
        }
        val (process, hasFinishedOnTime) = result

        // Don't use blocking read from stdout/stderr on non-finished process. If the process is hanging this would result in hanging test.
        val output = process.readOutput(outputFilter, nonBlocking = !hasFinishedOnTime)

        if (hasFinishedOnTime) {
            val exitCode: Int = process.exitValue()

            RunResult.Completed(exitCode, duration, output)
        } else {
            process.destroy() // Initiate destroy of non-finished process.
            Thread.sleep(5) // And give it a white to become actually destroyed.

            val exitCode: Int? = try {
                // If we are lucky enough, the process is destroyed to this moment. And it's possible to fetch exit code.
                process.exitValue()
            } catch (_: IllegalThreadStateException) {
                // Still not destroyed. Let's go further.
                null
            }

            RunResult.TimeoutExceeded(executionTimeout, exitCode, duration, output)
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
