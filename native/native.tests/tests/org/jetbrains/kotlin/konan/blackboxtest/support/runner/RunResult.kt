/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.util.TestOutputFilter
import kotlin.time.Duration

internal sealed interface RunResult {
    val exitCode: Int?
    val duration: Duration
    val processOutput: ProcessOutput

    data class Completed(
        override val exitCode: Int,
        override val duration: Duration,
        override val processOutput: ProcessOutput
    ) : RunResult

    data class TimeoutExceeded(
        val timeout: Duration,
        override val exitCode: Int?,
        override val duration: Duration,
        override val processOutput: ProcessOutput
    ) : RunResult
}

internal class ProcessOutput(val stdOut: TestOutputFilter.FilteredOutput, val stdErr: String)
