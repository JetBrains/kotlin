/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunCheck.ExecutionTimeout
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunCheck.ExitCode
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
}

internal class TestRunChecks(
    val executionTimeoutCheck: ExecutionTimeout,
    private val exitCodeCheck: ExitCode
) : Iterable<TestRunCheck> {

    override fun iterator() = iterator {
        yield(executionTimeoutCheck)
        yield(exitCodeCheck)
    }

    companion object {
        // The most frequently used case:
        @Suppress("TestFunctionName")
        fun Default(timeout: Duration) = TestRunChecks(
            ExecutionTimeout.ShouldNotExceed(timeout),
            ExitCode.Expected(0)
        )
    }
}
