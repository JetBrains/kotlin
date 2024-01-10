/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.time.TimeSource.Monotonic.ValueTimeMark
import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.withScopedMemoryAllocator

private const val MONOTONIC = 1

/**
 * Return the time value of a clock. Note: This is similar to `clock_gettime` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "clock_time_get")
private external fun wasiRawClockTimeGet(clockId: Int, precision: Long, resultPtr: Int): Int

private fun clockTimeGet(): Long = withScopedMemoryAllocator { allocator ->
    val rp0 = allocator.allocate(8)
    val ret = wasiRawClockTimeGet(
        clockId = MONOTONIC,
        precision = 1,
        resultPtr = rp0.address.toInt()
    )
    if (ret == 0) {
        rp0.loadLong()
    } else {
        throw WasiError(WasiErrorCode.entries[ret])
    }
}

@SinceKotlin("1.3")
internal actual object MonotonicTimeSource : TimeSource.WithComparableMarks {
    private val zero: Long = clockTimeGet()
    private fun read(): Long = clockTimeGet() - zero
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