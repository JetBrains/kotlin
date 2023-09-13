/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.*
import org.jetbrains.kotlin.utils.yieldIfNotNull
import java.io.File
import kotlin.time.Duration

internal sealed interface TestRunCheck {
    sealed class ExecutionTimeout(val timeout: Duration) : TestRunCheck {
        class ShouldNotExceed(timeout: Duration) : ExecutionTimeout(timeout)
        class ShouldExceed(timeout: Duration) : ExecutionTimeout(timeout)
    }

    sealed class ExitCode : TestRunCheck {
        object AnyNonZero : ExitCode()
        class Expected(val expectedExitCode: Int) : ExitCode()
    }

    class OutputDataFile(val file: File) : TestRunCheck

    class OutputMatcher(val match: (String) -> Boolean): TestRunCheck
}

internal class TestRunChecks(
    val executionTimeoutCheck: ExecutionTimeout,
    private val exitCodeCheck: ExitCode,
    val outputDataFile: OutputDataFile?,
    val outputMatcher: OutputMatcher?
) : Iterable<TestRunCheck> {

    override fun iterator() = iterator {
        yield(executionTimeoutCheck)
        yield(exitCodeCheck)
        yieldIfNotNull(outputDataFile)
        yieldIfNotNull(outputMatcher)
    }

    companion object {
        // The most frequently used case:
        @Suppress("TestFunctionName")
        fun Default(timeout: Duration) = TestRunChecks(
            executionTimeoutCheck = ExecutionTimeout.ShouldNotExceed(timeout),
            exitCodeCheck = ExitCode.Expected(0),
            outputDataFile = null,
            outputMatcher = null
        )
    }
}
