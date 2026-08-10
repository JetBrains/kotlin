/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.test.TestInfrastructureException
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.junit.jupiter.api.AssertionFailureBuilder
import org.opentest4j.FileInfo
import org.opentest4j.MultipleFailuresError
import org.opentest4j.TestAbortedException
import java.io.File
import java.nio.charset.StandardCharsets

abstract class AbstractRunner<R> : Runner<R> {
    protected abstract fun buildRun(): AbstractRun
    protected abstract fun buildResultHandler(runResult: RunResult): AbstractResultHandler<R>
    protected abstract fun getLoggedParameters(): LoggedData.TestRunParameters
    protected abstract fun handleUnexpectedFailure(t: Throwable): Nothing

    /**
     * The number of times [runAttempt] may be repeated if it does not succeed.
     *
     * @see TestRunParameter.WithRetriesOnFailure
     */
    protected open val maxRetries: Int get() = 0

    final override fun run(): R {
        val failures = mutableListOf<Throwable>()

        repeat(maxRetries + 1) { attempt ->
            try {
                return runAttempt()
            } catch (t: TestInfrastructureException) {
                // A broken test infrastructure invariant, not a flaky test run. Retrying it makes no sense.
                throw t.withSuppressed(failures)
            } catch (t: Throwable) {
                failures += t

                if (attempt < maxRetries) {
                    // Note: printing the whole failure would flood the log, because its message includes all the logged data
                    // (the compiler call, the entire output of the test executable, etc.).
                    // The failures of all the attempts are reported in full if the last attempt fails too.
                    println("Test run attempt ${attempt + 1} of ${maxRetries + 1} failed, retrying: ${t.shortDescription}")
                }
            }
        }

        // All the attempts have failed. Report the last failure, with the previous ones attached to it.
        throw failures.last().withSuppressed(failures.dropLast(1))
    }

    private fun runAttempt(): R = try {
        val run = buildRun()
        val runResult = run.run()
        val resultHandler = buildResultHandler(runResult)
        resultHandler.handle()
    } catch (t: Throwable) {
        when (t) {
            is AssertionError, is TestAbortedException, is TestInfrastructureException -> throw t
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

abstract class AbstractResultHandler<R>(protected val runResult: RunResult) {
    abstract fun getLoggedRun(): LoggedData
    abstract fun handle(): R

    protected fun verifyExpectation(shouldBeTrue: Boolean, failedResults: List<TestRunCheck.Result.Failed>, errorMessage: () -> String) {
        if (!shouldBeTrue) {
            val message = getLoggedRun().withErrorMessage(errorMessage())
            val causes = failedResults.mapNotNull { it.cause }
            throw MultipleFailuresError(message, causes)
        }
    }

    protected fun throwAssertionFailureWithExpectedFile(
        expectedFile: File,
        actual: String,
        errorMessage: String,
    ) {
        AssertionFailureBuilder.assertionFailure()
            .message(getLoggedRun().withErrorMessage(errorMessage))
            .expected(
                FileInfo(
                    expectedFile.absolutePath,
                    expectedFile.readText().convertLineSeparators().toByteArray(StandardCharsets.UTF_8)
                )
            )
            .actual(actual)
            .buildAndThrow()
    }
}

private fun Throwable.withSuppressed(failures: List<Throwable>): Throwable = apply { failures.forEach(::addSuppressed) }

/**
 * A short summary of the failure, as opposed to its [Throwable.message], which may include all the logged data.
 */
private val Throwable.shortDescription: String
    get() = "${this::class.java.name}: ${message?.lineSequence()?.firstOrNull()}"
