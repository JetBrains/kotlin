/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.util.Logger
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val ansiReset = "\u001B[0m"
private const val ansiTimeColor = "\u001B[36m"
private const val ansiTargetColor = "\u001B[32m"

internal inline fun <T> Logger?.progress(message: String, action: () -> T): T {
    if (this == null)
        return action()

    val timeMark = TimeSource.Monotonic.markNow()
    try {
        return action()
    } finally {
        val duration = timeMark.elapsedNow().toPrettyString()
        log("$message ${ansiTimeColor}in $duration$ansiReset")
    }
}

internal inline fun <T> Logger?.progress(
    target: CommonizerTarget, message: String, action: () -> T,
): T {
    return progress("[$ansiTargetColor$target$ansiReset]: $message", action)
}

private fun Duration.toPrettyString() = buildString {
    toComponents { hours, minutes, seconds, nanos ->
        // human-friendly formatted duration
        if (hours > 0) append(hours).append("h ")
        if (minutes > 0 || isNotEmpty()) append(minutes).append("m ")
        if (seconds > 0 || isNotEmpty()) append(seconds).append("s ")
        append(nanos / 1000_000).append("ms")
    }
}
