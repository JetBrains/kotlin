/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import kotlin.time.Duration

fun Executor.executeWithRepeatOnTimeout(request: ExecuteRequest, timeouts: List<Duration>): ExecuteResponse {
    require(timeouts.isNotEmpty())
    lateinit var response: ExecuteResponse

    for (timeout in timeouts) {
        response = execute(request.copying {
            this.timeout = timeout
        })
        if (response.exitCode != null) {
            return response
        } else {
            // It was killed by a timeout, let's repeat.
        }
    }

    return response
}
