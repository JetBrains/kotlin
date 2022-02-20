/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

// Long time reading saturation math, shared between JVM and Native

internal fun saturatingAdd(longNs: Long, duration: Duration): Long {
    val durationNs = duration.inWholeNanoseconds
    if ((longNs - 1) or 1 == Long.MAX_VALUE) { // MIN_VALUE or MAX_VALUE - the reading is infinite
        return checkInfiniteSumDefined(longNs, duration, durationNs)
    }
    if ((durationNs - 1) or 1 == Long.MAX_VALUE) { // duration doesn't fit in Long nanos
        return saturatingAddInHalves(longNs, duration)
    }

    val result = longNs + durationNs
    if (((longNs xor result) and (durationNs xor result)) < 0) {
        return if (longNs < 0) Long.MIN_VALUE else Long.MAX_VALUE
    }
    return result
}

private fun checkInfiniteSumDefined(longNs: Long, duration: Duration, durationNs: Long): Long {
    if (duration.isInfinite() && (longNs xor durationNs < 0)) throw IllegalArgumentException("Summing infinities of different signs")
    return longNs
}

private fun saturatingAddInHalves(longNs: Long, duration: Duration): Long {
    val half = duration / 2
    if ((half.inWholeNanoseconds - 1) or 1 == Long.MAX_VALUE) {
        // this will definitely saturate
        return (longNs + duration.toDouble(DurationUnit.NANOSECONDS)).toLong()
    } else {
        return saturatingAdd(saturatingAdd(longNs, half), half)
    }
}

internal fun saturatingDiff(valueNs: Long, originNs: Long): Duration {
    if ((originNs - 1) or 1 == Long.MAX_VALUE) { // MIN_VALUE or MAX_VALUE
        return -(originNs.toDuration(DurationUnit.DAYS)) // saturate to infinity
    }
    val result = valueNs - originNs
    if ((result xor valueNs) and (result xor originNs).inv() < 0) {
        val resultMs = valueNs / NANOS_IN_MILLIS - originNs / NANOS_IN_MILLIS
        val resultNs = valueNs % NANOS_IN_MILLIS - originNs % NANOS_IN_MILLIS
        return resultMs.milliseconds + resultNs.nanoseconds
    }
    return result.nanoseconds
}
