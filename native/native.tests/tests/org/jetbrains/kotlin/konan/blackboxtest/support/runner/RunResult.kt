/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import org.jetbrains.kotlin.konan.blackboxtest.support.util.TestOutputFilter
import kotlin.time.Duration

internal data class RunResult(
    val exitCode: Int?,
    val timeout: Duration,
    val duration: Duration,
    val hasFinishedOnTime: Boolean,
    val processOutput: ProcessOutput
) {
    init {
        // null exit code is possible only when test run hasn't finished on time.
        check(exitCode != null || !hasFinishedOnTime)
    }
}

internal class ProcessOutput(val stdOut: TestOutputFilter.FilteredOutput, val stdErr: String)
