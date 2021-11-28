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
@SinceKotlin("1.6")
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

@SinceKotlin("1.5")
internal fun durationUnitByShortName(shortName: String): DurationUnit = when (shortName) {
    "ns" -> DurationUnit.NANOSECONDS
    "us" -> DurationUnit.MICROSECONDS
    "ms" -> DurationUnit.MILLISECONDS
    "s" -> DurationUnit.SECONDS
    "m" -> DurationUnit.MINUTES
    "h" -> DurationUnit.HOURS
    "d" -> DurationUnit.DAYS
    else -> throw IllegalArgumentException("Unknown duration unit short name: $shortName")
}

@SinceKotlin("1.5")
internal fun durationUnitByIsoChar(isoChar: Char, isTimeComponent: Boolean): DurationUnit =
    when {
        !isTimeComponent -> {
            when (isoChar) {
                'D' -> DurationUnit.DAYS
                else -> throw IllegalArgumentException("Invalid or unsupported duration ISO non-time unit: $isoChar")
            }
        }
        else -> {
            when (isoChar) {
                'H' -> DurationUnit.HOURS
                'M' -> DurationUnit.MINUTES
                'S' -> DurationUnit.SECONDS
                else -> throw IllegalArgumentException("Invalid duration ISO time unit: $isoChar")
            }
        }
    }