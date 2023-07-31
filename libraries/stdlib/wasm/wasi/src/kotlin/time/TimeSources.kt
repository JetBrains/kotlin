/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource.Monotonic.ValueTimeMark
import kotlin.wasm.WasiError
import kotlin.wasm.WasiErrorCode
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.Pointer
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
        (Pointer(rp0.address.toInt().toUInt())).loadLong()
    } else {
        throw WasiError(WasiErrorCode.values()[ret])
    }
}

@SinceKotlin("1.3")
//TODO: Try use implementation of K/JVM since it uses a Long nanosecond counter similar to System.nanoTime (KT-60963)
internal actual object MonotonicTimeSource : TimeSource.WithComparableMarks {
    actual override fun markNow(): ValueTimeMark =
        ValueTimeMark(clockTimeGet())

    actual fun elapsedFrom(timeMark: ValueTimeMark): Duration =
        (clockTimeGet() - timeMark.reading).nanoseconds

    actual fun adjustReading(timeMark: ValueTimeMark, duration: Duration): ValueTimeMark =
        ValueTimeMark(timeMark.reading + duration.toLong(DurationUnit.NANOSECONDS))

    actual fun differenceBetween(one: ValueTimeMark, another: ValueTimeMark): Duration {
        val ms1 = one.reading
        val ms2 = another.reading
        return if (ms1 == ms2) Duration.ZERO else (ms1 - ms2).nanoseconds
    }

    override fun toString(): String = "WASI monotonic time source"
}

@Suppress("ACTUAL_WITHOUT_EXPECT") // visibility
internal actual typealias ValueTimeMarkReading = Long