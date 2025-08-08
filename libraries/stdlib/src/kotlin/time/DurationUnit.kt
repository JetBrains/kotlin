/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

@kotlin.internal.InlineOnly
internal inline fun String.durationUnitByShortNameOrNull(start: Int): DurationUnit? {
    val first = this[start]
    val second = if (start < lastIndex) this[start + 1] else '\u0000'

    return when (first) {
        'd' -> DurationUnit.DAYS
        'h' -> DurationUnit.HOURS
        's' -> DurationUnit.SECONDS
        'm' -> if (second == 's') DurationUnit.MILLISECONDS else DurationUnit.MINUTES
        'u' -> if (second == 's') DurationUnit.MICROSECONDS else null
        'n' -> if (second == 's') DurationUnit.NANOSECONDS else null
        else -> null
    }
}

@Suppress("REDUNDANT_ELSE_IN_WHEN")
internal val DurationUnit.multiplier: Double
    get() = when (this) {
        DurationUnit.NANOSECONDS -> 0.000000000000001
        DurationUnit.MICROSECONDS -> 0.000000000001
        DurationUnit.MILLISECONDS -> 0.000000001
        DurationUnit.SECONDS -> 0.000001
        DurationUnit.MINUTES -> 0.00006
        DurationUnit.HOURS -> 0.0036
        DurationUnit.DAYS -> 0.0864
        else -> error("Unknown unit: $this")
    }

internal val DurationUnit.length: Int
    get() = when (this) {
        DurationUnit.MILLISECONDS, DurationUnit.MICROSECONDS, DurationUnit.NANOSECONDS -> 2
        else -> 1
    }