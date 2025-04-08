/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

/**
 * Creates a [Clock] that uses the [time mark at the moment of creation][TimeMark.markNow] to determine how [far][TimeMark.elapsedNow]
 * the [current moment][Clock.now] is from the [origin].
 *
 * This clock stores the [TimeMark] at the moment of creation, so repeatedly creating [Clock]s from the same [TimeSource] results
 * in different [Instant]s iff the time of the [TimeSource] was increased. To sync different [Clock]s, use the [origin]
 * parameter.
 *
 * @sample samples.time.Clocks.timeSourceAsClock
 */
@SinceKotlin("2.2")
@ExperimentalTime
@kotlin.jvm.JvmName("fromTimeSource")
public fun TimeSource.asClock(origin: Instant): Clock = object : Clock {
    private val startMark: TimeMark = markNow()
    override fun now() = origin + startMark.elapsedNow()
}
