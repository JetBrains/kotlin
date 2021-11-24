/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import kotlin.time.Duration

sealed interface RunResult {
    data class Completed(val exitCode: Int, val duration: Duration, val stdOut: String, val stdErr: String) : RunResult
    data class TimeoutExceeded(val timeout: Duration) : RunResult
}
