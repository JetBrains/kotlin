/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import java.lang.System.currentTimeMillis

internal class ResettableClockMark {
    internal class Period(val start: Long, val end: Long) {
        override fun toString(): String {
            var remainder = end - start

            val millis = remainder % 1000
            remainder /= 1000

            val seconds = remainder % 60
            remainder /= 60

            val minutes = remainder % 60

            val hours = remainder / 60

            // human-friendly formatted duration
            return buildString {
                if (hours > 0) append(hours).append("h ")
                if (minutes > 0 || isNotEmpty()) append(minutes).append("m ")
                if (seconds > 0 || isNotEmpty()) append(seconds).append("s ")
                append(millis).append("ms")
            }
        }
    }

    private var startMark: Long = 0
    private var lastMark: Long = 0

    fun elapsedSinceLast(): Period = Period(lastMark, currentTimeMillis()).also { lastMark = it.end }
    fun elapsedSinceStart(): Period = Period(startMark, currentTimeMillis())

    fun reset() {
        startMark = currentTimeMillis()
        lastMark = startMark
    }
}

// TODO: this is how it should be when Kotlin Time will become non-experimental
//@ExperimentalTime
//internal class ResettableClockMark {
//    private val startMark = MonoClock.markNow()
//    private var lastMark = startMark
//
//    fun elapsedSinceLast(): Duration = lastMark.elapsedNow().also { lastMark = lastMark.plus(it) }
//    fun elapsedSinceStart(): Duration = startMark.elapsedNow()
//}
