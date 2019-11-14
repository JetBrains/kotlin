/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.math.abs

@UseExperimental(ExperimentalTime::class)
private inline val storageUnit get() = DurationUnit.NANOSECONDS

/**
 * Represents the amount of time one instant of time is away from another instant.
 *
 * A negative duration is possible in a situation when the second instant is earlier than the first one.
 * An infinite duration value [Duration.INFINITE] can be used to represent infinite timeouts.
 *
 * To construct a duration use either the extension function [toDuration],
 * or the extension properties [hours], [minutes], [seconds], and so on,
 * available on [Int], [Long], and [Double] numeric types.
 *
 * To get the value of this duration expressed in a particular [duration units][DurationUnit]
 * use the functions [toInt], [toLong], and [toDouble]
 * or the properties [inHours], [inMinutes], [inSeconds], [inNanoseconds], and so on.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public inline class Duration internal constructor(internal val value: Double) : Comparable<Duration> {
// TODO: backend fails on init block, wait for KT-28055

//    init {
//        require(_value.isNaN().not())
//    }

    companion object {
        /** The duration equal to exactly 0 seconds. */
        public val ZERO: Duration = Duration(0.0)

        /** The duration whose value is positive infinity. It is useful for representing timeouts that should never expire. */
        public val INFINITE: Duration = Duration(Double.POSITIVE_INFINITY)

        /** Converts the given time duration [value] expressed in the specified [sourceUnit] into the specified [targetUnit]. */
        public fun convert(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double =
            convertDurationUnit(value, sourceUnit, targetUnit)
    }

    // arithmetic operators

    /** Returns the negative of this value. */
    public operator fun unaryMinus(): Duration = Duration(-value)

    /** Returns a duration whose value is the sum of this and [other] duration values. */
    public operator fun plus(other: Duration): Duration = Duration(value + other.value)

    /** Returns a duration whose value is the difference between this and [other] duration values. */
    public operator fun minus(other: Duration): Duration = Duration(value - other.value)

    /** Returns a duration whose value is this duration value multiplied by the given [scale] number. */
    public operator fun times(scale: Int): Duration = Duration(value * scale)

    /** Returns a duration whose value is this duration value multiplied by the given [scale] number. */
    public operator fun times(scale: Double): Duration = Duration(value * scale)

    /** Returns a duration whose value is this duration value divided by the given [scale] number. */
    public operator fun div(scale: Int): Duration = Duration(value / scale)

    /** Returns a duration whose value is this duration value divided by the given [scale] number. */
    public operator fun div(scale: Double): Duration = Duration(value / scale)

    /** Returns a number that is the ratio of this and [other] duration values. */
    public operator fun div(other: Duration): Double = this.value / other.value

    /** Returns true, if the duration value is less than zero. */
    public fun isNegative(): Boolean = value < 0

    /** Returns true, if the duration value is greater than zero. */
    public fun isPositive(): Boolean = value > 0

    /** Returns true, if the duration value is infinite. */
    public fun isInfinite(): Boolean = value.isInfinite()

    /** Returns true, if the duration value is finite. */
    public fun isFinite(): Boolean = value.isFinite()

    /** Returns the absolute value of this value. The returned value is always non-negative. */
    public val absoluteValue: Duration get() = if (isNegative()) -this else this

    override fun compareTo(other: Duration): Int = this.value.compareTo(other.value)


    // splitting to components

    /**
     * Splits this duration into days, hours, minutes, seconds, and nanoseconds and executes the given [action] with these components.
     * The result of [action] is returned as the result of this function.
     *
     * - `nanoseconds` represents the whole number of nanoseconds in this duration, and its absolute value is less than 1_000_000_000;
     * - `seconds` represents the whole number of seconds in this duration, and its absolute value is less than 60;
     * - `minutes` represents the whole number of minutes in this duration, and its absolute value is less than 60;
     * - `hours` represents the whole number of hours in this duration, and its absolute value is less than 24;
     * - `days` represents the whole number of days in this duration.
     *   If the value doesn't fit in [Int] range, i.e. it's greater than [Int.MAX_VALUE] or less than [Int.MIN_VALUE],
     *   it is coerced into that range.
     */
    public inline fun <T> toComponents(action: (days: Int, hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T =
        action(inDays.toInt(), hoursComponent, minutesComponent, secondsComponent, nanosecondsComponent)

    /**
     * Splits this duration into hours, minutes, seconds, and nanoseconds and executes the given [action] with these components.
     * The result of [action] is returned as the result of this function.
     *
     * - `nanoseconds` represents the whole number of nanoseconds in this duration, and its absolute value is less than 1_000_000_000;
     * - `seconds` represents the whole number of seconds in this duration, and its absolute value is less than 60;
     * - `minutes` represents the whole number of minutes in this duration, and its absolute value is less than 60;
     * - `hours` represents the whole number of hours in this duration.
     *   If the value doesn't fit in [Int] range, i.e. it's greater than [Int.MAX_VALUE] or less than [Int.MIN_VALUE],
     *   it is coerced into that range.
     */
    public inline fun <T> toComponents(action: (hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T =
        action(inHours.toInt(), minutesComponent, secondsComponent, nanosecondsComponent)

    /**
     * Splits this duration into minutes, seconds, and nanoseconds and executes the given [action] with these components.
     * The result of [action] is returned as the result of this function.
     *
     * - `nanoseconds` represents the whole number of nanoseconds in this duration, and its absolute value is less than 1_000_000_000;
     * - `seconds` represents the whole number of seconds in this duration, and its absolute value is less than 60;
     * - `minutes` represents the whole number of minutes in this duration.
     *   If the value doesn't fit in [Int] range, i.e. it's greater than [Int.MAX_VALUE] or less than [Int.MIN_VALUE],
     *   it is coerced into that range.
     */
    public inline fun <T> toComponents(action: (minutes: Int, seconds: Int, nanoseconds: Int) -> T): T =
        action(inMinutes.toInt(), secondsComponent, nanosecondsComponent)

    /**
     * Splits this duration into seconds, and nanoseconds and executes the given [action] with these components.
     * The result of [action] is returned as the result of this function.
     *
     * - `nanoseconds` represents the whole number of nanoseconds in this duration, and its absolute value is less than 1_000_000_000;
     * - `seconds` represents the whole number of seconds in this duration.
     *   If the value doesn't fit in [Long] range, i.e. it's greater than [Long.MAX_VALUE] or less than [Long.MIN_VALUE],
     *   it is coerced into that range.
     */
    public inline fun <T> toComponents(action: (seconds: Long, nanoseconds: Int) -> T): T =
        action(inSeconds.toLong(), nanosecondsComponent)

    @PublishedApi
    internal val hoursComponent: Int get() = (inHours % 24).toInt()
    @PublishedApi
    internal val minutesComponent: Int get() = (inMinutes % 60).toInt()
    @PublishedApi
    internal val secondsComponent: Int get() = (inSeconds % 60).toInt()
    @PublishedApi
    internal val nanosecondsComponent: Int get() = (inNanoseconds % 1e9).toInt()


    // conversion to units

    /** Returns the value of this duration expressed as a [Double] number of the specified [unit]. */
    public fun toDouble(unit: DurationUnit): Double = convertDurationUnit(value, storageUnit, unit)

    /**
     * Returns the value of this duration expressed as a [Long] number of the specified [unit].
     *
     * If the value doesn't fit in the range of [Long] type, it is coerced into that range, see the conversion [Double.toLong] for details.
     */
    public fun toLong(unit: DurationUnit): Long = toDouble(unit).toLong()

    /**
     * Returns the value of this duration expressed as an [Int] number of the specified [unit].
     *
     * If the value doesn't fit in the range of [Int] type, it is coerced into that range, see the conversion [Double.toInt] for details.
     */
    public fun toInt(unit: DurationUnit): Int = toDouble(unit).toInt()

    /** The value of this duration expressed as a [Double] number of days. */
    public val inDays: Double get() = toDouble(DurationUnit.DAYS)

    /** The value of this duration expressed as a [Double] number of hours. */
    public val inHours: Double get() = toDouble(DurationUnit.HOURS)

    /** The value of this duration expressed as a [Double] number of minutes. */
    public val inMinutes: Double get() = toDouble(DurationUnit.MINUTES)

    /** The value of this duration expressed as a [Double] number of seconds. */
    public val inSeconds: Double get() = toDouble(DurationUnit.SECONDS)

    /** The value of this duration expressed as a [Double] number of milliseconds. */
    public val inMilliseconds: Double get() = toDouble(DurationUnit.MILLISECONDS)

    /** The value of this duration expressed as a [Double] number of microseconds. */
    public val inMicroseconds: Double get() = toDouble(DurationUnit.MICROSECONDS)

    /** The value of this duration expressed as a [Double] number of nanoseconds. */
    public val inNanoseconds: Double get() = toDouble(DurationUnit.NANOSECONDS)

    // shortcuts

    /**
     * Returns the value of this duration expressed as a [Long] number of nanoseconds.
     *
     * If the value doesn't fit in the range of [Long] type, it is coerced into that range, see the conversion [Double.toLong] for details.
     *
     * The range of durations that can be expressed as a `Long` number of nanoseconds is approximately ±292 years.
     */
    public fun toLongNanoseconds(): Long = toLong(DurationUnit.NANOSECONDS)

    /**
     * Returns the value of this duration expressed as a [Long] number of milliseconds.
     *
     * The value is coerced to the range of [Long] type, if it doesn't fit in that range, see the conversion [Double.toLong] for details.
     *
     * The range of durations that can be expressed as a `Long` number of milliseconds is approximately ±292 million years.
     */
    public fun toLongMilliseconds(): Long = toLong(DurationUnit.MILLISECONDS)

    /**
     * Returns a string representation of this duration value expressed in the unit which yields the most compact and readable number value.
     *
     * Special cases:
     *  - zero duration is formatted as `"0s"`
     *  - the infinite duration is formatted as `"Infinity"` without unit
     *  - very small durations (less than 1e-15 s) are expressed in seconds and formatted in scientific notation
     *  - very big durations (more than 1e+7 days) are expressed in days and formatted in scientific notation
     *
     * @return the value of duration in the automatically determined unit followed by that unit abbreviated name: `d`, `h`, `m`, `s`, `ms`, `us`, or `ns`.
     *
     * @sample samples.time.Durations.toStringDefault
     */
    override fun toString(): String = when {
        isInfinite() -> value.toString()
        value == 0.0 -> "0s"
        else -> {
            val absNs = absoluteValue.inNanoseconds
            var scientific = false
            var maxDecimals = 0
            val unit = when {
                absNs < 1e-6 -> DurationUnit.SECONDS.also { scientific = true }
                absNs < 1 -> DurationUnit.NANOSECONDS.also { maxDecimals = 7 }
                absNs < 1e3 -> DurationUnit.NANOSECONDS
                absNs < 1e6 -> DurationUnit.MICROSECONDS
                absNs < 1e9 -> DurationUnit.MILLISECONDS
                absNs < 1000e9 -> DurationUnit.SECONDS
                absNs < 60_000e9 -> DurationUnit.MINUTES
                absNs < 3600_000e9 -> DurationUnit.HOURS
                absNs < 86400e9 * 1e7 -> DurationUnit.DAYS
                else -> DurationUnit.DAYS.also { scientific = true }
            }
            val value = toDouble(unit)
            when {
                scientific -> formatScientific(value)
                maxDecimals > 0 -> formatUpToDecimals(value, maxDecimals)
                else -> formatToExactDecimals(value, precision(abs(value)))
            } + unit.shortName()
        }
    }

    private fun precision(value: Double): Int = when {
        value < 1 -> 3
        value < 10 -> 2
        value < 100 -> 1
        else -> 0
    }

    /**
     * Returns a string representation of this duration value expressed in the given [unit]
     * and formatted with the specified [decimals] number of digits after decimal point.
     *
     * Special cases:
     *  - the infinite duration is formatted as `"Infinity"` without unit
     *
     * @return the value of duration in the specified [unit] followed by that unit abbreviated name: `d`, `h`, `m`, `s`, `ms`, `us`, or `ns`.
     *
     * @throws IllegalArgumentException if [decimals] is less than zero.
     *
     * @sample samples.time.Durations.toStringDecimals
     */
    public fun toString(unit: DurationUnit, decimals: Int = 0): String {
        require(decimals >= 0) { "decimals must be not negative, but was $decimals" }
        if (isInfinite()) return value.toString()
        val number = toDouble(unit)
        return when {
            abs(number) < 1e14 -> formatToExactDecimals(number, decimals.coerceAtMost(12))
            else -> formatScientific(number)
        } + unit.shortName()
    }


    /**
     * Returns an ISO-8601 based string representation of this duration.
     *
     * The returned value is presented in the format `PThHmMs.fS`, where `h`, `m`, `s` are the integer components of this duration (see [toComponents])
     * and `f` is a fractional part of second. Depending on the roundness of the value the fractional part can be formatted with either
     * 0, 3, 6, or 9 decimal digits.
     *
     * If the hours component absolute value of this duration is greater than [Int.MAX_VALUE], it is replaced with [Int.MAX_VALUE],
     * so the infinite duration is formatted as `"PT2147483647H".
     *
     * Negative durations are indicated with the sign `-` in the beginning of the returned string, for example, `"-PT5M30S"`.
     *
     * @sample samples.time.Durations.toIsoString
     */
    @UseExperimental(ExperimentalStdlibApi::class)
    public fun toIsoString(): String = buildString {
        if (isNegative()) append('-')
        append("PT")
        absoluteValue.toComponents { hours, minutes, seconds, nanoseconds ->
            val hasHours = hours != 0
            val hasSeconds = seconds != 0 || nanoseconds != 0
            val hasMinutes = minutes != 0 || (hasSeconds && hasHours)
            if (hasHours) {
                append(hours).append('H')
            }
            if (hasMinutes) {
                append(minutes).append('M')
            }
            if (hasSeconds || (!hasHours && !hasMinutes)) {
                append(seconds)
                if (nanoseconds != 0) {
                    append('.')
                    val nss = nanoseconds.toString().padStart(9, '0')
                    when {
                        nanoseconds % 1_000_000 == 0 -> appendRange(nss, 0, 3)
                        nanoseconds % 1_000 == 0 -> appendRange(nss, 0, 6)
                        else -> append(nss)
                    }
                }
                append('S')
            }
        }
    }

}

// constructing from number of units
// extension functions

/** Returns a [Duration] equal to this [Int] number of the specified [unit]. */
@SinceKotlin("1.3")
@ExperimentalTime
public fun Int.toDuration(unit: DurationUnit): Duration = toDouble().toDuration(unit)

/** Returns a [Duration] equal to this [Long] number of the specified [unit]. */
@SinceKotlin("1.3")
@ExperimentalTime
public fun Long.toDuration(unit: DurationUnit): Duration = toDouble().toDuration(unit)

/** Returns a [Duration] equal to this [Double] number of the specified [unit]. */
@SinceKotlin("1.3")
@ExperimentalTime
public fun Double.toDuration(unit: DurationUnit): Duration = Duration(convertDurationUnit(this, unit, storageUnit))

// constructing from number of units
// extension properties

/** Returns a [Duration] equal to this [Int] number of nanoseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Int.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

/** Returns a [Duration] equal to this [Long] number of nanoseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Long.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

/** Returns a [Duration] equal to this [Double] number of nanoseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Double.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

/** Returns a [Duration] equal to this [Int] number of microseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Int.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

/** Returns a [Duration] equal to this [Long] number of microseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Long.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

/** Returns a [Duration] equal to this [Double] number of microseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Double.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

/** Returns a [Duration] equal to this [Int] number of milliseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Int.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

/** Returns a [Duration] equal to this [Long] number of milliseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Long.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

/** Returns a [Duration] equal to this [Double] number of milliseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Double.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

/** Returns a [Duration] equal to this [Int] number of seconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Int.seconds get() = toDuration(DurationUnit.SECONDS)

/** Returns a [Duration] equal to this [Long] number of seconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Long.seconds get() = toDuration(DurationUnit.SECONDS)

/** Returns a [Duration] equal to this [Double] number of seconds. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Double.seconds get() = toDuration(DurationUnit.SECONDS)

/** Returns a [Duration] equal to this [Int] number of minutes. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Int.minutes get() = toDuration(DurationUnit.MINUTES)

/** Returns a [Duration] equal to this [Long] number of minutes. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Long.minutes get() = toDuration(DurationUnit.MINUTES)

/** Returns a [Duration] equal to this [Double] number of minutes. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Double.minutes get() = toDuration(DurationUnit.MINUTES)

/** Returns a [Duration] equal to this [Int] number of hours. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Int.hours get() = toDuration(DurationUnit.HOURS)

/** Returns a [Duration] equal to this [Long] number of hours. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Long.hours get() = toDuration(DurationUnit.HOURS)

/** Returns a [Duration] equal to this [Double] number of hours. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Double.hours get() = toDuration(DurationUnit.HOURS)

/** Returns a [Duration] equal to this [Int] number of days. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Int.days get() = toDuration(DurationUnit.DAYS)

/** Returns a [Duration] equal to this [Long] number of days. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Long.days get() = toDuration(DurationUnit.DAYS)

/** Returns a [Duration] equal to this [Double] number of days. */
@SinceKotlin("1.3")
@ExperimentalTime
public val Double.days get() = toDuration(DurationUnit.DAYS)


/** Returns a duration whose value is the specified [duration] value multiplied by this number. */
@SinceKotlin("1.3")
@ExperimentalTime
@kotlin.internal.InlineOnly
public inline operator fun Int.times(duration: Duration): Duration = duration * this

/** Returns a duration whose value is the specified [duration] value multiplied by this number. */
@SinceKotlin("1.3")
@ExperimentalTime
@kotlin.internal.InlineOnly
public inline operator fun Double.times(duration: Duration): Duration = duration * this


internal expect fun formatToExactDecimals(value: Double, decimals: Int): String
internal expect fun formatUpToDecimals(value: Double, decimals: Int): String
internal expect fun formatScientific(value: Double): String