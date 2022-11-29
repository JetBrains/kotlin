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
    if (longNs.isSaturated()) { // MIN_VALUE or MAX_VALUE - the reading is infinite
        return checkInfiniteSumDefined(longNs, duration, durationNs)
    }
    if (durationNs.isSaturated()) { // duration doesn't fit in Long nanos
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
    if (half.inWholeNanoseconds.isSaturated()) {
        // this will definitely saturate
        return (longNs + duration.toDouble(DurationUnit.NANOSECONDS)).toLong()
    } else {
        return saturatingAdd(saturatingAdd(longNs, half), duration - half)
    }
}

internal fun saturatingDiff(valueNs: Long, originNs: Long): Duration {
    if (originNs.isSaturated()) { // MIN_VALUE or MAX_VALUE
        return -(originNs.toDuration(DurationUnit.DAYS)) // saturate to infinity
    }
    return saturatingFiniteDiff(valueNs, originNs)
}

internal fun saturatingOriginsDiff(origin1Ns: Long, origin2Ns: Long): Duration {
    if (origin2Ns.isSaturated()) { // MIN_VALUE or MAX_VALUE
        if (origin1Ns == origin2Ns) return Duration.ZERO // saturated values of the same sign are considered equal
        return -(origin2Ns.toDuration(DurationUnit.DAYS)) // saturate to infinity
    }
    if (origin1Ns.isSaturated()) {
        return origin1Ns.toDuration(DurationUnit.DAYS)
    }
    return saturatingFiniteDiff(origin1Ns, origin2Ns)
}

private fun saturatingFiniteDiff(value1Ns: Long, value2Ns: Long): Duration {
    val result = value1Ns - value2Ns
    if ((result xor value1Ns) and (result xor value2Ns).inv() < 0) {
        val resultMs = value1Ns / NANOS_IN_MILLIS - value2Ns / NANOS_IN_MILLIS
        val resultNs = value1Ns % NANOS_IN_MILLIS - value2Ns % NANOS_IN_MILLIS
        return resultMs.milliseconds + resultNs.nanoseconds
    }
    return result.nanoseconds
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Long.isSaturated(): Boolean =
    (this - 1) or 1 == Long.MAX_VALUE // == either MAX_VALUE or MIN_VALUE
