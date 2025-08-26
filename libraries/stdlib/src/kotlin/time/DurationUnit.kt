/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass()
@file:kotlin.jvm.JvmName("DurationUnitKt")

package kotlin.time

import kotlin.math.abs


/**
 * The list of possible time measurement units, in which a duration can be expressed.
 *
 * The smallest time unit is [NANOSECONDS] and the largest is [DAYS], which corresponds to exactly 24 [HOURS].
 */
@WasExperimental(ExperimentalTime::class)
public expect enum class DurationUnit {
    /**
     * Time unit representing one nanosecond, which is 1/1000 of a microsecond.
     */
    NANOSECONDS,
    /**
     * Time unit representing one microsecond, which is 1/1000 of a millisecond.
     */
    MICROSECONDS,
    /**
     * Time unit representing one millisecond, which is 1/1000 of a second.
     */
    MILLISECONDS,
    /**
     * Time unit representing one second.
     */
    SECONDS,
    /**
     * Time unit representing one minute.
     */
    MINUTES,
    /**
     * Time unit representing one hour.
     */
    HOURS,
    /**
     * Time unit representing one day, which is always equal to 24 hours.
     */
    DAYS;
}

/** Converts the given time duration [value] expressed in the specified [sourceUnit] into the specified [targetUnit]. */
@SinceKotlin("1.3")
internal expect fun convertDurationUnit(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double

// overflown result is unspecified
@SinceKotlin("1.5")
internal expect fun convertDurationUnitOverflow(value: Long, sourceUnit: DurationUnit, targetUnit: DurationUnit): Long

// overflown result is coerced in the Long range boundaries
@SinceKotlin("1.5")
internal expect fun convertDurationUnit(value: Long, sourceUnit: DurationUnit, targetUnit: DurationUnit): Long

/**
 * Converts a duration [value] from the specified [unit] to milliseconds.
 *
 * This function performs the conversion by multiplying the value with the unit's
 * millisecond multiplier, using overflow-safe multiplication.
 *
 * @param value the duration value to convert
 * @param unit the source duration unit
 * @return the duration value converted to milliseconds
 */
@kotlin.internal.InlineOnly
internal inline fun convertDurationUnitToMilliseconds(value: Long, unit: DurationUnit): Long =
    value.multiplyWithoutOverflow(unit.millisMultiplier)

/**
 * Checks if multiplying two Long values exceeds [MAX_MILLIS] bounds.
 *
 * Uses a bit-counting technique to determine if the product of [a] and [b]
 * would exceed the bounds of a 64-bit signed integer without actually
 * performing the multiplication.
 *
 * @return true if [a] * [b] would overflow [MAX_MILLIS] or -[MAX_MILLIS]
 */
private fun willMultiplyOverflow(a: Long, b: Long): Boolean {
    val leadingZerosA = abs(a).countLeadingZeroBits()
    val leadingZerosB = abs(b).countLeadingZeroBits()
    return (64 - leadingZerosA) + (64 - leadingZerosB) > 63
}

/**
 * Multiplies this Long by another, clamping the result to ±[MAX_MILLIS] on overflow.
 * @return the product or ±[MAX_MILLIS] if overflow occurs
 */
private fun Long.multiplyWithoutOverflow(other: Long): Long = when {
    willMultiplyOverflow(this, other) -> if (this > 0) MAX_MILLIS else -MAX_MILLIS
    else -> this * other
}

/**
 * Number of milliseconds in one unit of this DurationUnit.
 * Used for converting whole unit values to milliseconds during parsing.
 */
private val DurationUnit.millisMultiplier: Long
    get() = when (this) {
        DurationUnit.DAYS -> MILLIS_IN_DAY
        DurationUnit.HOURS -> MILLIS_IN_HOUR
        DurationUnit.MINUTES -> MILLIS_IN_MINUTE
        DurationUnit.SECONDS -> MILLIS_IN_SECOND
        DurationUnit.MILLISECONDS -> 1L
        else -> error("Wrong unit for millisMultiplier: $this")
    }

@SinceKotlin("1.3")
@Suppress("REDUNDANT_ELSE_IN_WHEN")
internal fun DurationUnit.shortName(): String = when (this) {
    DurationUnit.NANOSECONDS -> "ns"
    DurationUnit.MICROSECONDS -> "us"
    DurationUnit.MILLISECONDS -> "ms"
    DurationUnit.SECONDS -> "s"
    DurationUnit.MINUTES -> "m"
    DurationUnit.HOURS -> "h"
    DurationUnit.DAYS -> "d"
    else -> error("Unknown unit: $this")
}
