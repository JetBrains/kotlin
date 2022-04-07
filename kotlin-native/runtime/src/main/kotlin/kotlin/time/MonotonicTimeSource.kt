/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.system.*

@SinceKotlin("1.3")
@ExperimentalTime
internal actual object MonotonicTimeSource : TimeSource {
    private val zero: Long = getTimeNanos()
    private fun read(): Long = getTimeNanos() - zero
    override fun toString(): String = "TimeSource(System.nanoTime())"

    actual override fun markNow(): DefaultTimeMark = DefaultTimeMark(read())
    actual fun elapsedFrom(timeMark: DefaultTimeMark): Duration =
            saturatingDiff(read(), timeMark.reading)

    actual fun adjustReading(timeMark: DefaultTimeMark, duration: Duration): DefaultTimeMark =
            DefaultTimeMark(saturatingAdd(timeMark.reading, duration))
}

@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias DefaultTimeMarkReading = Long
