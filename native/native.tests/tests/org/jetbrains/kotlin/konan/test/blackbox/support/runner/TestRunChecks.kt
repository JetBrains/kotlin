/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
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

    object ExpectedFailure : TestRunCheck

    class OutputDataFile(val file: File) : TestRunCheck

    class OutputMatcher(val match: (String) -> Boolean): TestRunCheck

    class FileCheckMatcher(val settings: Settings, val testDataFile: File): TestRunCheck
}

internal data class TestRunChecks(
    val executionTimeoutCheck: ExecutionTimeout,
    private val exitCodeCheck: ExitCode?,
    val expectedFailureCheck: ExpectedFailure?,
    val outputDataFile: OutputDataFile?,
    val outputMatcher: OutputMatcher?,
    val fileCheckMatcher: FileCheckMatcher?,
) : Iterable<TestRunCheck> {

    override fun iterator() = iterator {
        yield(executionTimeoutCheck)
        yieldIfNotNull(exitCodeCheck)
        yieldIfNotNull(expectedFailureCheck)
        yieldIfNotNull(outputDataFile)
        yieldIfNotNull(outputMatcher)
        yieldIfNotNull(fileCheckMatcher)
    }

    companion object {
        // The most frequently used case:
        @Suppress("TestFunctionName")
        fun Default(timeout: Duration) = TestRunChecks(
            executionTimeoutCheck = ExecutionTimeout.ShouldNotExceed(timeout),
            exitCodeCheck = ExitCode.Expected(0),
            expectedFailureCheck = null,
            outputDataFile = null,
            outputMatcher = null,
            fileCheckMatcher = null,
        )
    }
}
