/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import kotlin.time.Duration

internal sealed interface RunResult {
    val exitCode: Int?
    val duration: Duration
    val output: ProcessOutput

    data class Completed(
        override val exitCode: Int,
        override val duration: Duration,
        override val output: ProcessOutput
    ) : RunResult

    data class TimeoutExceeded(
        val timeout: Duration,
        override val exitCode: Int?,
        override val duration: Duration,
        override val output: ProcessOutput
    ) : RunResult
}

internal class ProcessOutput(val stdOut: String, val stdErr: String)
