/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.AbstractRunner.AbstractRun
import kotlin.time.*

internal abstract class AbstractLocalProcessRunner<R>(private val executionTimeout: Duration) : AbstractRunner<R>() {
    protected abstract val visibleProcessName: String
    protected abstract val executable: TestExecutable
    protected abstract val programArgs: List<String>

    protected open fun customizeProcess(process: Process) = Unit

    @OptIn(ExperimentalTime::class)
    final override fun buildRun() = AbstractRun {
        val exitCode: Int

        val stdOut: String
        val stdErr: String

        val duration = measureTime {
            val process = ProcessBuilder(programArgs).directory(executable.executableFile.parentFile).start()
            customizeProcess(process)

            val hasFinishedInTime = process.waitFor(
                executionTimeout.toLong(DurationUnit.MILLISECONDS),
                DurationUnit.MILLISECONDS.toTimeUnit()
            )

            if (!hasFinishedInTime) {
                process.destroy()
                return@AbstractRun RunResult.TimeoutExceeded(executionTimeout)
            }

            exitCode = process.exitValue()

            stdOut = process.inputStream.bufferedReader().readText()
            stdErr = process.errorStream.bufferedReader().readText()
        }

        RunResult.Completed(exitCode, duration, stdOut, stdErr)
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
