/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass()
@file:kotlin.jvm.JvmName("DurationUnitKt")

package kotlin.time


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
 * @param value the duration value to convert. Should always be non-negative.
 * @param unit the source duration unit
 * @return the duration value converted to milliseconds
 */
internal fun convertDurationUnitToMilliseconds(value: Long, unit: DurationUnit): Long =
    value.multiplyNonNegativeWithoutOverflow(unit.millisMultiplier)

/**
 * Multiplies this non-negative Long value by another positive Long value,
 * clamping the result to [MAX_MILLIS] on overflow.
 *
 * This function optimizes multiplication for non-negative values by:
 * - Handling special cases (0 and 1) efficiently
 * - Using bit counting to detect potential overflow without performing the multiplication
 * - Clamping to [MAX_MILLIS] when overflow is detected
 *
 * @param other the Long value to multiply by (always positive)
 * @return the product clamped to the range [0, [MAX_MILLIS]]
 */
private fun Long.multiplyNonNegativeWithoutOverflow(other: Long): Long = when {
    this == 0L -> 0L
    this == 1L -> other.coerceAtMost(MAX_MILLIS)
    other == 1L -> this.coerceAtMost(MAX_MILLIS)
    else -> {
        val bitSum = 128 - countLeadingZeroBits() - other.countLeadingZeroBits()
        when {
            bitSum < 63 -> this * other
            bitSum > 63 -> MAX_MILLIS
            else -> (this * other).coerceAtMost(MAX_MILLIS)
        }
    }
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
