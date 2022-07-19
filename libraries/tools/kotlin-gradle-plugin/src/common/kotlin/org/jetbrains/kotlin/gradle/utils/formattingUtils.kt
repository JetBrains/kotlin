/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.logging.Logger

internal fun formatDuration(milliseconds: Long): String {
    var ms: Long = milliseconds
    var s: Long = 0
    var m: Long = 0
    var h: Long = 0

    if (ms >= 1000) {
        s = ms / 1000
        ms %= 1000
        if (s >= 60) {
            m = s / 60
            s %= 60
            if (m >= 60) {
                h = m / 60
                m %= 60
            }
        }
    }

    return buildString {
        if (h > 0) append(h).append(" h ")
        if (m > 0 || isNotEmpty()) append(m).append(" m ")
        if (s > 0 || isNotEmpty()) append(s).append(" s ")
        if (ms > 0 || isNotEmpty()) append(ms).append(" ms")
    }
}

internal inline fun <T> Logger.lifecycleWithDuration(messagePrefix: String, action: () -> T): T {
    val startTime = System.currentTimeMillis()
    val result = action()
    val finishTime = System.currentTimeMillis()

    lifecycle("$messagePrefix took ${formatDuration(finishTime - startTime)}")

    return result
}

internal fun formatContentLength(bytes: Long): String = when {
    bytes < 0 -> "N/A"
    bytes < 1024 -> "$bytes bytes"
    else -> {
        val kilobytes = bytes.toDouble() / 1024
        when {
            kilobytes < 1024 -> String.format("%.2f KB", kilobytes)
            else -> {
                val megabytes = kilobytes / 1024
                when {
                    megabytes < 1024 -> String.format("%.2f MB", megabytes)
                    else -> {
                        val gigabytes = megabytes / 1024
                        String.format("%.2f GB", gigabytes)
                    }
                }
            }
        }
    }
}

internal fun formatTime(ms: Long): String {
    val seconds = ms.toDouble() / 1_000
    return seconds.asString(2) + " s"
}

private const val kbSize = 1024
private const val mbSize = kbSize * 1024
private const val gbSize = mbSize * 1024

internal fun formatSize(sizeInBytes: Long): String = when {
    sizeInBytes / gbSize >= 1 -> "${(sizeInBytes.toDouble() / gbSize).asString(1)} GB"
    sizeInBytes / mbSize >= 1 -> "${(sizeInBytes.toDouble() / mbSize).asString(1)} MB"
    sizeInBytes / kbSize >= 1 -> "${(sizeInBytes.toDouble() / kbSize).asString(1)} KB"
    else -> "$sizeInBytes B"
}

internal fun Double.asString(decPoints: Int): String = "%,.${decPoints}f".format(this)