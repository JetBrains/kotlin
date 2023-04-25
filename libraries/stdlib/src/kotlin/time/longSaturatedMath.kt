/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.time.Duration.Companion.milliseconds

// Long time reading saturation math, shared between JVM and Native

internal fun saturatingAdd(value: Long, unit: DurationUnit, duration: Duration): Long {
    val durationInUnit = duration.toLong(unit)
    if (value.isSaturated()) { // the reading is infinitely saturated
        return checkInfiniteSumDefined(value, duration, durationInUnit)
    }
    if (durationInUnit.isSaturated()) { // duration doesn't fit in Long units
        return saturatingAddInHalves(value, unit, duration)
    }

    val result = value + durationInUnit
    if (((value xor result) and (durationInUnit xor result)) < 0) {
        return if (value < 0) Long.MIN_VALUE else Long.MAX_VALUE
    }
    return result
}

private fun checkInfiniteSumDefined(value: Long, duration: Duration, durationInUnit: Long): Long {
    if (duration.isInfinite() && (value xor durationInUnit < 0)) throw IllegalArgumentException("Summing infinities of different signs")
    return value
}

private fun saturatingAddInHalves(value: Long, unit: DurationUnit, duration: Duration): Long {
    val half = duration / 2
    val halfInUnit = half.toLong(unit)
    if (halfInUnit.isSaturated()) {
        return halfInUnit // value + inf == inf, return saturated value
    } else {
        return saturatingAdd(saturatingAdd(value, unit, half), unit, duration - half)
    }
}

private fun infinityOfSign(value: Long): Duration = if (value < 0) Duration.NEG_INFINITE else Duration.INFINITE

internal fun saturatingDiff(valueNs: Long, origin: Long, unit: DurationUnit): Duration {
    if (origin.isSaturated()) { // MIN_VALUE or MAX_VALUE
        return -infinityOfSign(origin)
    }
    return saturatingFiniteDiff(valueNs, origin, unit)
}

internal fun saturatingOriginsDiff(origin1: Long, origin2: Long, unit: DurationUnit): Duration {
    if (origin2.isSaturated()) {
        if (origin1 == origin2) return Duration.ZERO // saturated values of the same sign are considered equal
        return -infinityOfSign(origin2)
    }
    if (origin1.isSaturated()) {
        return infinityOfSign(origin1)
    }
    return saturatingFiniteDiff(origin1, origin2, unit)
}

private fun saturatingFiniteDiff(value1: Long, value2: Long, unit: DurationUnit): Duration {
    val result = value1 - value2
    if ((result xor value1) and (result xor value2).inv() < 0) { // Long overflow
        if (unit < DurationUnit.MILLISECONDS) {
            val unitsInMilli = convertDurationUnit(1, DurationUnit.MILLISECONDS, unit)
            val resultMs = value1 / unitsInMilli - value2 / unitsInMilli
            val resultUnit = value1 % unitsInMilli - value2 % unitsInMilli
            return resultMs.milliseconds + resultUnit.toDuration(unit)
        } else {
            return -infinityOfSign(result)
        }
    }
    return result.toDuration(unit)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.isSaturated(): Boolean =
    (this - 1) or 1 == Long.MAX_VALUE // == either MAX_VALUE or MIN_VALUE
