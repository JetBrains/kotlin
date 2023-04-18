/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.time.TimeSource.Monotonic.ValueTimeMark

@SinceKotlin("1.3")
internal actual object MonotonicTimeSource : TimeSource.WithComparableMarks {
    private val zero: Long = System.nanoTime()
    private fun read(): Long = System.nanoTime() - zero
    override fun toString(): String = "TimeSource(System.nanoTime())"

    actual override fun markNow(): ValueTimeMark = ValueTimeMark(read())
    actual fun elapsedFrom(timeMark: ValueTimeMark): Duration =
        saturatingDiff(read(), timeMark.reading, DurationUnit.NANOSECONDS)

    actual fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration =
        saturatingOriginsDiff(one.reading, another.reading, DurationUnit.NANOSECONDS)

    actual fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        ValueTimeMark(saturatingAdd(timeMark.reading, DurationUnit.NANOSECONDS, duration))
}

@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias ValueTimeMarkReading = Long
