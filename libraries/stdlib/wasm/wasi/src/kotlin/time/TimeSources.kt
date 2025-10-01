/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.internal.InlineOnly
import kotlin.time.TimeSource.Monotonic.ValueTimeMark
import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.WasmImport
import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * The clock measuring real time. Time value zero corresponds with 1970-01-01T00:00:00Z.
 */
private const val CLOCK_ID_REALTIME = 0

/**
 * The store-wide monotonic clock, which is defined as a clock measuring real time,
 * whose value cannot be adjusted and which cannot have negative clock jumps.
 * The epoch of this clock is undefined. The absolute time value of this clock therefore has no meaning.
 */
private const val CLOCK_ID_MONOTONIC = 1

/**
 * Returns timestamp of the given clock in nanoseconds.
 */
@OptIn(ExperimentalWasmInterop::class)
private fun clockTimeGet(clockId: Int): Long = throw UnsupportedOperationException("Clock not supported in Wasm Spec")

@InlineOnly
internal inline fun realtimeClockTimeGet() = clockTimeGet(CLOCK_ID_REALTIME)

@InlineOnly
private inline fun monotonicClockTimeGet() = clockTimeGet(CLOCK_ID_MONOTONIC)

@SinceKotlin("1.3")
internal actual object MonotonicTimeSource : TimeSource.WithComparableMarks {
    private val zero: Long = monotonicClockTimeGet()
    private fun read(): Long = monotonicClockTimeGet() - zero
    override fun toString(): String = "TimeSource(clock_time_get())"

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
