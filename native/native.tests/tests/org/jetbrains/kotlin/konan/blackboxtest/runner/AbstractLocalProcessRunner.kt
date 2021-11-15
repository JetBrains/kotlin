/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.runner

import org.jetbrains.kotlin.konan.blackboxtest.TestExecutable

internal abstract class AbstractLocalProcessRunner<R> : AbstractRunner<R>() {
    protected abstract val visibleProcessName: String
    protected abstract val executable: TestExecutable
    protected abstract val programArgs: List<String>

    protected open fun customizeProcess(process: Process) = Unit

    final override fun buildRun() = object : AbstractRun {
        override fun run(): RunResult {
            val startTimeMillis = System.currentTimeMillis()

            val process = ProcessBuilder(programArgs).directory(executable.executableFile.parentFile).start()
            customizeProcess(process)

            val exitCode = process.waitFor()
            val finishTimeMillis = System.currentTimeMillis()

            val stdOut = process.inputStream.bufferedReader().readText()
            val stdErr = process.errorStream.bufferedReader().readText()

            return RunResult(exitCode, finishTimeMillis - startTimeMillis, stdOut, stdErr)
        }
    }

    abstract override fun buildResultHandler(runResult: RunResult): ResultHandler // Narrow returned type.

    abstract inner class ResultHandler(runResult: RunResult) : AbstractRunner<R>.ResultHandler(runResult) {
        override fun handle(): R {
            verifyExpectation(0, runResult.exitCode) { "$visibleProcessName exited with non-zero code." }

            return doHandle()
        }

        protected abstract fun doHandle(): R
    }
}
