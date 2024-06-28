/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.opentest4j.TestAbortedException

internal abstract class AbstractRunner<R> : Runner<R> {
    protected abstract fun buildRun(): AbstractRun
    protected abstract fun buildResultHandler(runResult: RunResult): AbstractResultHandler<R>
    protected abstract fun getLoggedParameters(): LoggedData.TestRunParameters
    protected abstract fun handleUnexpectedFailure(t: Throwable): Nothing

    final override fun run(): R = try {
        val run = buildRun()
        val runResult = run.run()
        val resultHandler = buildResultHandler(runResult)
        resultHandler.handle()
    } catch (t: Throwable) {
        when (t) {
            is AssertionError, is TestAbortedException -> throw t
            else -> {
                // An unexpected failure.
                handleUnexpectedFailure(t)
            }
        }
    }

    fun interface AbstractRun {
        fun run(): RunResult
    }
}

internal abstract class AbstractResultHandler<R>(protected val runResult: RunResult) {
    abstract fun getLoggedRun(): LoggedData
    abstract fun handle(): R

    protected inline fun verifyExpectation(shouldBeTrue: Boolean, crossinline errorMessage: () -> String) {
        assertTrue(shouldBeTrue) { getLoggedRun().withErrorMessage(errorMessage()) }
    }
}
