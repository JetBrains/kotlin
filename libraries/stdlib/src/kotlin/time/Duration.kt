/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.contracts.*
import kotlin.jvm.JvmInline
import kotlin.math.*

/**
 * Represents the amount of time one instant of time is away from another instant.
 *
 * A negative duration is possible in a situation when the second instant is earlier than the first one.
 *
 * The type can store duration values up to ±146 years with nanosecond precision,
 * and up to ±146 million years with millisecond precision.
 * If a duration-returning operation provided in `kotlin.time` produces a duration value that doesn't fit into the above range,
 * the returned `Duration` is infinite.
 *
 * An infinite duration value [Duration.INFINITE] can be used to represent infinite timeouts.
 *
 * To construct a duration use either the extension function [toDuration],
 * or the extension properties [hours], [minutes], [seconds], and so on,
 * available on [Int], [Long], and [Double] numeric types.
 *
 * To get the value of this duration expressed in a particular [duration units][DurationUnit]
 * use the functions [toInt], [toLong], and [toDouble]
 * or the properties [inWholeHours], [inWholeMinutes], [inWholeSeconds], [inWholeNanoseconds], and so on.
 */
@SinceKotlin("1.6")
@JvmInline
public value class Duration internal constructor(private val rawValue: Long) : Comparable<Duration> {

    private val value: Long get() = rawValue shr 1
    private inline val unitDiscriminator: Int get() = rawValue.toInt() and 1
    private fun isInNanos() = unitDiscriminator == 0
    private fun isInMillis() = unitDiscriminator == 1
    private val storageUnit get() = if (isInNanos()) DurationUnit.NANOSECONDS else DurationUnit.MILLISECONDS

    init {
        if (durationAssertionsEnabled) {
            if (isInNanos()) {
                if (value !in -MAX_NANOS..MAX_NANOS) throw AssertionError("$value ns is out of nanoseconds range")
            } else {
                if (value !in -MAX_MILLIS..MAX_MILLIS) throw AssertionError("$value ms is out of milliseconds range")
                if (value in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS) throw AssertionError("$value ms is denormalized")
            }
        }
    }

    public companion object {
        /** The duration equal to exactly 0 seconds. */
        public val ZERO: Duration = Duration(0L)

        /** The duration whose value is positive infinity. It is useful for representing timeouts that should never expire. */
        public val INFINITE: Duration = durationOfMillis(MAX_MILLIS)
        internal val NEG_INFINITE: Duration = durationOfMillis(-MAX_MILLIS)

        internal const val INVALID_RAW_VALUE = 0x7FFFFFFFFBADC0DE
        internal val INVALID: Duration = Duration(INVALID_RAW_VALUE)

        internal const val SUMMING_INFINITE_DURATIONS_OF_DIFFERENT_SIGN_ERROR_MESSAGE =
            "Summing infinite durations of different signs yields an undefined result."

        /** Converts the given time duration [value] expressed in the specified [sourceUnit] into the specified [targetUnit]. */
        @ExperimentalTime
        public fun convert(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double =
            convertDurationUnit(value, sourceUnit, targetUnit)

        // Duration construction extension properties in Duration companion scope

        /**
         * Returns a [Duration] equal to this [Int] number of nanoseconds.
         *
         * @sample samples.time.Durations.fromNanoseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Int.nanoseconds: Duration get() = toDuration(DurationUnit.NANOSECONDS)

        /**
         * Returns a [Duration] equal to this [Long] number of nanoseconds.
         *
         * @sample samples.time.Durations.fromNanoseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Long.nanoseconds: Duration get() = toDuration(DurationUnit.NANOSECONDS)

        /**
         * Returns a [Duration] equal to this [Double] number of nanoseconds.
         *
         * Depending on its magnitude, the value is rounded to an integer number of nanoseconds or milliseconds.
         *
         * @throws IllegalArgumentException if this [Double] value is `NaN`.
         * @sample samples.time.Durations.fromNanoseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Double.nanoseconds: Duration get() = toDuration(DurationUnit.NANOSECONDS)


        /**
         * Returns a [Duration] equal to this [Int] number of microseconds.
         *
         * @sample samples.time.Durations.fromMicroseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Int.microseconds: Duration get() = toDuration(DurationUnit.MICROSECONDS)

        /**
         * Returns a [Duration] equal to this [Long] number of microseconds.
         *
         * @sample samples.time.Durations.fromMicroseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Long.microseconds: Duration get() = toDuration(DurationUnit.MICROSECONDS)

        /**
         * Returns a [Duration] equal to this [Double] number of microseconds.
         *
         * Depending on its magnitude, the value is rounded to an integer number of nanoseconds or milliseconds.
         *
         * @throws IllegalArgumentException if this [Double] value is `NaN`.
         * @sample samples.time.Durations.fromMicroseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Double.microseconds: Duration get() = toDuration(DurationUnit.MICROSECONDS)


        /**
         * Returns a [Duration] equal to this [Int] number of milliseconds.
         *
         * @sample samples.time.Durations.fromMilliseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Int.milliseconds: Duration get() = toDuration(DurationUnit.MILLISECONDS)

        /**
         * Returns a [Duration] equal to this [Long] number of milliseconds.
         *
         * @sample samples.time.Durations.fromMilliseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Long.milliseconds: Duration get() = toDuration(DurationUnit.MILLISECONDS)

        /**
         * Returns a [Duration] equal to this [Double] number of milliseconds.
         *
         * Depending on its magnitude, the value is rounded to an integer number of nanoseconds or milliseconds.
         *
         * @throws IllegalArgumentException if this [Double] value is `NaN`.
         * @sample samples.time.Durations.fromMilliseconds
         */
        @kotlin.internal.InlineOnly
        public inline val Double.milliseconds: Duration get() = toDuration(DurationUnit.MILLISECONDS)


        /**
         * Returns a [Duration] equal to this [Int] number of seconds.
         *
         * @sample samples.time.Durations.fromSeconds
         */
        @kotlin.internal.InlineOnly
        public inline val Int.seconds: Duration get() = toDuration(DurationUnit.SECONDS)

        /**
         * Returns a [Duration] equal to this [Long] number of seconds.
         *
         * @sample samples.time.Durations.fromSeconds
         */
        @kotlin.internal.InlineOnly
        public inline val Long.seconds: Duration get() = toDuration(DurationUnit.SECONDS)

        /**
         * Returns a [Duration] equal to this [Double] number of seconds.
         *
         * Depending on its magnitude, the value is rounded to an integer number of nanoseconds or milliseconds.
         *
         * @throws IllegalArgumentException if this [Double] value is `NaN`.
         * @sample samples.time.Durations.fromSeconds
         */
        @kotlin.internal.InlineOnly
        public inline val Double.seconds: Duration get() = toDuration(DurationUnit.SECONDS)


        /**
         * Returns a [Duration] equal to this [Int] number of minutes.
         *
         * @sample samples.time.Durations.fromMinutes
         */
        @kotlin.internal.InlineOnly
        public inline val Int.minutes: Duration get() = toDuration(DurationUnit.MINUTES)

        /**
         * Returns a [Duration] equal to this [Long] number of minutes.
         *
         * @sample samples.time.Durations.fromMinutes
         */
        @kotlin.internal.InlineOnly
        public inline val Long.minutes: Duration get() = toDuration(DurationUnit.MINUTES)

        /**
         * Returns a [Duration] equal to this [Double] number of minutes.
         *
         * Depending on its magnitude, the value is rounded to an integer number of nanoseconds or milliseconds.
         *
         * @throws IllegalArgumentException if this [Double] value is `NaN`.
         * @sample samples.time.Durations.fromMinutes
         */
        @kotlin.internal.InlineOnly
        public inline val Double.minutes: Duration get() = toDuration(DurationUnit.MINUTES)


        /**
         * Returns a [Duration] equal to this [Int] number of hours.
         *
         * @sample samples.time.Durations.fromHours
         */
        @kotlin.internal.InlineOnly
        public inline val Int.hours: Duration get() = toDuration(DurationUnit.HOURS)

        /**
         * Returns a [Duration] equal to this [Long] number of hours.
         *
         * @sample samples.time.Durations.fromHours
         */
        @kotlin.internal.InlineOnly
        public inline val Long.hours: Duration get() = toDuration(DurationUnit.HOURS)

        /**
         * Returns a [Duration] equal to this [Double] number of hours.
         *
         * Depending on its magnitude, the value is rounded to an integer number of nanoseconds or milliseconds.
         *
         * @throws IllegalArgumentException if this [Double] value is `NaN`.
         * @sample samples.time.Durations.fromHours
         */
        @kotlin.internal.InlineOnly
        public inline val Double.hours: Duration get() = toDuration(DurationUnit.HOURS)


        /**
         * Returns a [Duration] equal to this [Int] number of days.
         *
         * Note that a day in this conversion always represents exactly 24 hours.
         * This is different from calendar days which may be longer or shorter
         * than 24 hours when a daylight saving transition happens on that day.
         *
         * @sample samples.time.Durations.fromDays
         */
        @kotlin.internal.InlineOnly
        public inline val Int.days: Duration get() = toDuration(DurationUnit.DAYS)

        /**
         * Returns a [Duration] equal to this [Long] number of days.
         *
         * Note that a day in this conversion always represents exactly 24 hours.
         * This is different from calendar days which may be longer or shorter
         * than 24 hours when a daylight saving transition happens on that day.
         *
         * @sample samples.time.Durations.fromDays
         */
        @kotlin.internal.InlineOnly
        public inline val Long.days: Duration get() = toDuration(DurationUnit.DAYS)

        /**
         * Returns a [Duration] equal to this [Double] number of days.
         *
         * Depending on its magnitude, the value is rounded to an integer number of nanoseconds or milliseconds.
         *
         * Note that a day in this conversion always represents exactly 24 hours.
         * This is different from calendar days which may be longer or shorter
         * than 24 hours when a daylight saving transition happens on that day.
         *
         * @throws IllegalArgumentException if this [Double] value is `NaN`.
         * @sample samples.time.Durations.fromDays
         */
        @kotlin.internal.InlineOnly
        public inline val Double.days: Duration get() = toDuration(DurationUnit.DAYS)


        /**
         * Parses a string that represents a duration and returns the parsed [Duration] value.
         *
         * The following formats are accepted:
         *
         * - ISO-8601 Duration format, e.g. `P1DT2H3M4.058S`, see [toIsoString] and [parseIsoString].
         * - The format of string returned by the default [Duration.toString] and `toString` in a specific unit,
         *   e.g. `10s`, `1h 30m` or `-(1h 30m)`.
         *
         * @throws IllegalArgumentException if the string doesn't represent a duration in any of the supported formats.
         * @sample samples.time.Durations.parse
         */
        public fun parse(value: String): Duration = try {
            parseDuration(value, strictIso = false)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid duration string format: '$value'.", e)
        }

        /**
         * Parses a string that represents a duration in a restricted ISO-8601 composite representation
         * and returns the parsed [Duration] value.
         * Composite representation is a relaxed version of ISO-8601 duration format that supports
         * negative durations and negative values of individual components.
         *
         * The following restrictions are imposed:
         *
         * - The only allowed non-time designator is days (`D`). `Y` (years), `W` (weeks), and `M` (months) are not supported.
         * - Day is considered to be exactly 24 hours (24-hour clock time scale).
         * - Alternative week-based representation `["P"][number]["W"]` is not supported.
         *
         * @throws IllegalArgumentException if the string doesn't represent a duration in ISO-8601 format.
         * @sample samples.time.Durations.parseIsoString
         */
        public fun parseIsoString(value: String): Duration = try {
            parseDuration(value, strictIso = true)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid ISO duration string format: '$value'.", e)
        }

        /**
         * Parses a string that represents a duration and returns the parsed [Duration] value,
         * or `null` if the string doesn't represent a duration in any of the supported formats.
         *
         * The following formats are accepted:
         *
         * - Restricted ISO-8601 duration composite representation, e.g. `P1DT2H3M4.058S`, see [toIsoString] and [parseIsoString].
         * - The format of string returned by the default [Duration.toString] and `toString` in a specific unit,
         *   e.g. `10s`, `1h 30m` or `-(1h 30m)`.
         *   @sample samples.time.Durations.parse
         */
        public fun parseOrNull(value: String): Duration? =
            parseDuration(value, strictIso = false, throwException = false).let { if (it == INVALID) null else it }

        /**
         * Parses a string that represents a duration in restricted ISO-8601 composite representation
         * and returns the parsed [Duration] value or `null` if the string doesn't represent a duration in the format
         * acceptable by [parseIsoString].
         *
         * @sample samples.time.Durations.parseIsoString
         */
        public fun parseIsoStringOrNull(value: String): Duration? =
            parseDuration(value, strictIso = true, throwException = false).let { if (it == INVALID) null else it }
    }

    // arithmetic operators

    /** Returns the negative of this value. */
    public operator fun unaryMinus(): Duration = durationOf(-value, unitDiscriminator)

    /**
     * Returns a duration whose value is the sum of this and [other] duration values.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * e.g. when adding infinite durations of different sign.
     */
    public operator fun plus(other: Duration): Duration {
        when {
            this.isInfinite() -> {
                if (other.isFinite() || (this.rawValue xor other.rawValue >= 0))
                    return this
                else
                    throw IllegalArgumentException(SUMMING_INFINITE_DURATIONS_OF_DIFFERENT_SIGN_ERROR_MESSAGE)
            }
            other.isInfinite() -> return other
        }

        return addFiniteDurations(other)
    }

    @kotlin.internal.InlineOnly
    internal inline fun plus(other: Duration, throwException: Boolean): Duration {
        when {
            this.isInfinite() -> {
                if (other.isFinite() || (this.rawValue xor other.rawValue >= 0))
                    return this
                else
                    if (throwException) throw IllegalArgumentException(SUMMING_INFINITE_DURATIONS_OF_DIFFERENT_SIGN_ERROR_MESSAGE) else return INVALID
            }
            other.isInfinite() -> return other
        }

        return addFiniteDurations(other)
    }

    private fun addFiniteDurations(other: Duration): Duration {
        return when {
            this.unitDiscriminator == other.unitDiscriminator -> {
                val result = this.value + other.value // never overflows long, but can overflow long63
                when {
                    isInNanos() ->
                        durationOfNanosNormalized(result)
                    else ->
                        durationOfMillisNormalized(result)
                }
            }
            this.isInMillis() ->
                addValuesMixedRanges(this.value, other.value)
            else ->
                addValuesMixedRanges(other.value, this.value)
        }
    }

    private fun addValuesMixedRanges(thisMillis: Long, otherNanos: Long): Duration {
        val otherMillis = nanosToMillis(otherNanos)
        val resultMillis = thisMillis + otherMillis
        return if (resultMillis in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS) {
            val otherNanoRemainder = otherNanos - millisToNanos(otherMillis)
            durationOfNanos(millisToNanos(resultMillis) + otherNanoRemainder)
        } else {
            durationOfMillis(resultMillis.coerceIn(-MAX_MILLIS, MAX_MILLIS))
        }
    }

    /**
     * Returns a duration whose value is the difference between this and [other] duration values.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * e.g. when subtracting infinite durations of the same sign.
     */
    public operator fun minus(other: Duration): Duration = this + (-other)

    /**
     * Returns a duration whose value is this duration value multiplied by the given [scale] number.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * e.g. when multiplying an infinite duration by zero.
     */
    public operator fun times(scale: Int): Duration {
        if (isInfinite()) {
            return when {
                scale == 0 -> throw IllegalArgumentException("Multiplying infinite duration by zero yields an undefined result.")
                scale > 0 -> this
                else -> -this
            }
        }
        if (scale == 0) return ZERO

        val value = value
        val result = value * scale
        return if (isInNanos()) {
            if (value in (MAX_NANOS / Int.MIN_VALUE)..(-MAX_NANOS / Int.MIN_VALUE)) {
                // can't overflow nanos range for any scale
                durationOfNanos(result)
            } else {
                if (result / scale == value) {
                    durationOfNanosNormalized(result)
                } else {
                    val millis = nanosToMillis(value)
                    val remNanos = value - millisToNanos(millis)
                    val resultMillis = millis * scale
                    val totalMillis = resultMillis + nanosToMillis(remNanos * scale)
                    if (resultMillis / scale == millis && totalMillis xor resultMillis >= 0) {
                        durationOfMillis(totalMillis.coerceIn(-MAX_MILLIS..MAX_MILLIS))
                    } else {
                        if (value.sign * scale.sign > 0) INFINITE else NEG_INFINITE
                    }
                }
            }
        } else {
            if (result / scale == value) {
                durationOfMillis(result.coerceIn(-MAX_MILLIS..MAX_MILLIS))
            } else {
                if (value.sign * scale.sign > 0) INFINITE else NEG_INFINITE
            }
        }
    }

    /**
     * Returns a duration whose value is this duration value multiplied by the given [scale] number.
     *
     * The operation may involve rounding when the result cannot be represented exactly with a [Double] number.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * e.g. when multiplying an infinite duration by zero.
     */
    public operator fun times(scale: Double): Duration {
        val intScale = scale.roundToInt()
        if (intScale.toDouble() == scale) {
            return times(intScale)
        }

        val unit = storageUnit
        val result = toDouble(unit) * scale
        return result.toDuration(unit)
    }

    /**
     * Returns a duration whose value is this duration value divided by the given [scale] number.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * e.g. when dividing zero duration by zero.
     */
    public operator fun div(scale: Int): Duration {
        if (scale == 0) {
            return when {
                isPositive() -> INFINITE
                isNegative() -> NEG_INFINITE
                else -> throw IllegalArgumentException("Dividing zero duration by zero yields an undefined result.")
            }
        }
        if (isInNanos()) {
            return durationOfNanos(value / scale)
        } else {
            if (isInfinite())
                return this * scale.sign

            val result = value / scale

            if (result in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS) {
                val rem = millisToNanos(value - (result * scale)) / scale
                return durationOfNanos(millisToNanos(result) + rem)
            }
            return durationOfMillis(result)
        }
    }

    /**
     * Returns a duration whose value is this duration value divided by the given [scale] number.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * e.g. when dividing an infinite duration by infinity or zero duration by zero.
     */
    public operator fun div(scale: Double): Duration {
        val intScale = scale.roundToInt()
        if (intScale.toDouble() == scale && intScale != 0) {
            return div(intScale)
        }

        val unit = storageUnit
        val result = toDouble(unit) / scale
        return result.toDuration(unit)
    }

    /** Returns a number that is the ratio of this and [other] duration values. */
    public operator fun div(other: Duration): Double {
        val coarserUnit = maxOf(this.storageUnit, other.storageUnit)
        return this.toDouble(coarserUnit) / other.toDouble(coarserUnit)
    }

    /**
     * Returns a duration whose value is this duration value truncated to the specified duration [unit].
     */
    internal fun truncateTo(unit: DurationUnit): Duration {
        val storageUnit = storageUnit
        if (unit <= storageUnit || this.isInfinite()) return this
        val scale = convertDurationUnit(1, unit, storageUnit)
        val result = value - value % scale
        return result.toDuration(storageUnit)
    }

    /** Returns true, if the duration value is less than zero. */
    public fun isNegative(): Boolean = rawValue < 0

    /** Returns true, if the duration value is greater than zero. */
    public fun isPositive(): Boolean = rawValue > 0

    /** Returns true, if the duration value is infinite. */
    public fun isInfinite(): Boolean = rawValue == INFINITE.rawValue || rawValue == NEG_INFINITE.rawValue

    /** Returns true, if the duration value is finite. */
    public fun isFinite(): Boolean = !isInfinite()

    /** Returns the absolute value of this value. The returned value is always non-negative. */
    public val absoluteValue: Duration get() = if (isNegative()) -this else this

    override fun compareTo(other: Duration): Int {
        val compareBits = this.rawValue xor other.rawValue
        if (compareBits < 0 || compareBits.toInt() and 1 == 0) // different signs or same sign/same range
            return this.rawValue.compareTo(other.rawValue)
        // same sign/different ranges
        val r = this.unitDiscriminator - other.unitDiscriminator // compare ranges
        return if (isNegative()) -r else r
    }


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
     *
     * Note that a day in this conversion always represents exactly 24 hours.
     * This is different from calendar days which may be longer or shorter
     * than 24 hours when a daylight saving transition happens on that day.
     *
     *   Infinite durations are represented as either [Long.MAX_VALUE] days, or [Long.MIN_VALUE] days (depending on the sign of infinity),
     *   and zeroes in the lower components.
     */
    public inline fun <T> toComponents(action: (days: Long, hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
        return action(inWholeDays, hoursComponent, minutesComponent, secondsComponent, nanosecondsComponent)
    }

    /**
     * Splits this duration into hours, minutes, seconds, and nanoseconds and executes the given [action] with these components.
     * The result of [action] is returned as the result of this function.
     *
     * - `nanoseconds` represents the whole number of nanoseconds in this duration, and its absolute value is less than 1_000_000_000;
     * - `seconds` represents the whole number of seconds in this duration, and its absolute value is less than 60;
     * - `minutes` represents the whole number of minutes in this duration, and its absolute value is less than 60;
     * - `hours` represents the whole number of hours in this duration.
     *
     *   Infinite durations are represented as either [Long.MAX_VALUE] hours, or [Long.MIN_VALUE] hours (depending on the sign of infinity),
     *   and zeroes in the lower components.
     */
    public inline fun <T> toComponents(action: (hours: Long, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
        return action(inWholeHours, minutesComponent, secondsComponent, nanosecondsComponent)
    }

    /**
     * Splits this duration into minutes, seconds, and nanoseconds and executes the given [action] with these components.
     * The result of [action] is returned as the result of this function.
     *
     * - `nanoseconds` represents the whole number of nanoseconds in this duration, and its absolute value is less than 1_000_000_000;
     * - `seconds` represents the whole number of seconds in this duration, and its absolute value is less than 60;
     * - `minutes` represents the whole number of minutes in this duration.
     *
     *   Infinite durations are represented as either [Long.MAX_VALUE] minutes, or [Long.MIN_VALUE] minutes (depending on the sign of infinity),
     *   and zeroes in the lower components.
     */
    public inline fun <T> toComponents(action: (minutes: Long, seconds: Int, nanoseconds: Int) -> T): T {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
        return action(inWholeMinutes, secondsComponent, nanosecondsComponent)
    }

    /**
     * Splits this duration into seconds, and nanoseconds and executes the given [action] with these components.
     * The result of [action] is returned as the result of this function.
     *
     * - `nanoseconds` represents the whole number of nanoseconds in this duration, and its absolute value is less than 1_000_000_000;
     * - `seconds` represents the whole number of seconds in this duration.
     *
     *   Infinite durations are represented as either [Long.MAX_VALUE] seconds, or [Long.MIN_VALUE] seconds (depending on the sign of infinity),
     *   and zero nanoseconds.
     */
    public inline fun <T> toComponents(action: (seconds: Long, nanoseconds: Int) -> T): T {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
        return action(inWholeSeconds, nanosecondsComponent)
    }

    @PublishedApi
    internal val hoursComponent: Int
        get() = if (isInfinite()) 0 else (inWholeHours % 24).toInt()

    @PublishedApi
    internal val minutesComponent: Int
        get() = if (isInfinite()) 0 else (inWholeMinutes % 60).toInt()

    @PublishedApi
    internal val secondsComponent: Int
        get() = if (isInfinite()) 0 else (inWholeSeconds % 60).toInt()

    @PublishedApi
    internal val nanosecondsComponent: Int
        get() = when {
            isInfinite() -> 0
            isInMillis() -> millisToNanos(value % 1_000).toInt()
            else -> (value % 1_000_000_000).toInt()
        }


    // conversion to units

    /**
     * Returns the value of this duration expressed as a [Double] number of the specified [unit].
     *
     * The operation may involve rounding when the result cannot be represented exactly with a [Double] number.
     *
     * An infinite duration value is converted either to [Double.POSITIVE_INFINITY] or [Double.NEGATIVE_INFINITY] depending on its sign.
     *
     * @sample samples.time.Durations.toDoubleUnits
     */
    public fun toDouble(unit: DurationUnit): Double {
        return when (rawValue) {
            INFINITE.rawValue -> Double.POSITIVE_INFINITY
            NEG_INFINITE.rawValue -> Double.NEGATIVE_INFINITY
            else -> {
                // TODO: whether it's ok to convert to Double before scaling
                convertDurationUnit(value.toDouble(), storageUnit, unit)
            }
        }
    }

    /**
     * Returns the value of this duration expressed as a [Long] number of the specified [unit].
     *
     * The part of this duration that is smaller than the specified unit
     * becomes a fractional part of the result and then is truncated (rounded towards zero).
     *
     * If the result doesn't fit in the range of [Long] type, it is coerced into that range:
     * - [Long.MIN_VALUE] is returned if it's less than `Long.MIN_VALUE`,
     * - [Long.MAX_VALUE] is returned if it's greater than `Long.MAX_VALUE`.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.toLongUnits
     */
    public fun toLong(unit: DurationUnit): Long {
        return when (rawValue) {
            INFINITE.rawValue -> Long.MAX_VALUE
            NEG_INFINITE.rawValue -> Long.MIN_VALUE
            else -> convertDurationUnit(value, storageUnit, unit)
        }
    }

    /**
     * Returns the value of this duration expressed as an [Int] number of the specified [unit].
     *
     * The part of this duration that is smaller than the specified unit
     * becomes a fractional part of the result and then is truncated (rounded towards zero).
     *
     * If the result doesn't fit in the range of [Int] type, it is coerced into that range:
     * - [Int.MIN_VALUE] is returned if it's less than `Int.MIN_VALUE`,
     * - [Int.MAX_VALUE] is returned if it's greater than `Int.MAX_VALUE`.
     *
     * An infinite duration value is converted either to [Int.MAX_VALUE] or [Int.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.toIntUnits
     */
    public fun toInt(unit: DurationUnit): Int =
        toLong(unit).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()


    /**
     * The value of this duration expressed as a [Long] number of days.
     *
     * The part of this duration that is smaller than a day
     * becomes a fractional part of the result and then is truncated (rounded towards zero).
     *
     * Note that a day in this conversion always represents exactly 24 hours.
     * This is different from calendar days which may be longer or shorter
     * than 24 hours when a daylight saving transition happens on that day.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.inWholeDays
     */
    public val inWholeDays: Long
        get() = toLong(DurationUnit.DAYS)

    /**
     * The value of this duration expressed as a [Long] number of hours.
     *
     * The part of this duration that is smaller than an hour
     * becomes a fractional part of the result and then is truncated (rounded towards zero).
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.inWholeHours
     */
    public val inWholeHours: Long
        get() = toLong(DurationUnit.HOURS)

    /**
     * The value of this duration expressed as a [Long] number of minutes.
     *
     * The part of this duration that is smaller than a minute
     * becomes a fractional part of the result and then is truncated (rounded towards zero).
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.inWholeMinutes
     */
    public val inWholeMinutes: Long
        get() = toLong(DurationUnit.MINUTES)

    /**
     * The value of this duration expressed as a [Long] number of seconds.
     *
     * The part of this duration that is smaller than a second
     * becomes a fractional part of the result and then is truncated (rounded towards zero).
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.inWholeSeconds
     */
    public val inWholeSeconds: Long
        get() = toLong(DurationUnit.SECONDS)

    /**
     * The value of this duration expressed as a [Long] number of milliseconds.
     *
     * The part of this duration that is smaller than a millisecond
     * becomes a fractional part of the result and then is truncated (rounded towards zero).
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.inWholeMilliseconds
     */
    public val inWholeMilliseconds: Long
        get() {
            return if (isInMillis() && isFinite()) value else toLong(DurationUnit.MILLISECONDS)
        }

    /**
     * The value of this duration expressed as a [Long] number of microseconds.
     *
     * The part of this duration that is smaller than a microsecond
     * becomes a fractional part of the result and then is truncated (rounded towards zero).
     *
     * If the result doesn't fit in the range of [Long] type, it is coerced into that range:
     * - [Long.MIN_VALUE] is returned if it's less than `Long.MIN_VALUE`,
     * - [Long.MAX_VALUE] is returned if it's greater than `Long.MAX_VALUE`.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.inWholeMicroseconds
     */
    public val inWholeMicroseconds: Long
        get() = toLong(DurationUnit.MICROSECONDS)

    /**
     * The value of this duration expressed as a [Long] number of nanoseconds.
     *
     * If the result doesn't fit in the range of [Long] type, it is coerced into that range:
     * - [Long.MIN_VALUE] is returned if it's less than `Long.MIN_VALUE`,
     * - [Long.MAX_VALUE] is returned if it's greater than `Long.MAX_VALUE`.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     *
     * @sample samples.time.Durations.inWholeNanoseconds
     */
    public val inWholeNanoseconds: Long
        get() {
            val value = value
            return when {
                isInNanos() -> value
                value > Long.MAX_VALUE / NANOS_IN_MILLIS -> Long.MAX_VALUE
                value < Long.MIN_VALUE / NANOS_IN_MILLIS -> Long.MIN_VALUE
                else -> millisToNanos(value)
            }
        }

    /**
     * Returns a string representation of this duration value
     * expressed as a combination of numeric components, each in its own unit.
     *
     * Each component is a number followed by the unit abbreviated name: `d`, `h`, `m`, `s`:
     * `5h`, `1d 12h`, `1h 0m 30.340s`.
     * The last component, usually seconds, can be a number with a fractional part.
     *
     * If the duration is less than a second, it is represented as a single number
     * with one of sub-second units: `ms` (milliseconds), `us` (microseconds), or `ns` (nanoseconds):
     * `140.884ms`, `500us`, `24ns`.
     *
     * A negative duration is prefixed with `-` sign and, if it consists of multiple components, surrounded with parentheses:
     * `-12m` and `-(1h 30m)`.
     *
     * Special cases:
     *  - an infinite duration is formatted as `"Infinity"` or `"-Infinity"` without a unit.
     *
     * It's recommended to use [toIsoString] that uses more strict ISO-8601 format instead of this `toString`
     * when you want to convert a duration to a string in cases of serialization, interchange, etc.
     *
     * @sample samples.time.Durations.toStringDefault
     */
    override fun toString(): String = when (rawValue) {
        0L -> "0s"
        INFINITE.rawValue -> "Infinity"
        NEG_INFINITE.rawValue -> "-Infinity"
        else -> {
            val isNegative = isNegative()
            buildString {
                if (isNegative) append('-')
                absoluteValue.toComponents { days, hours, minutes, seconds, nanoseconds ->
                    val hasDays = days != 0L
                    val hasHours = hours != 0
                    val hasMinutes = minutes != 0
                    val hasSeconds = seconds != 0 || nanoseconds != 0
                    var components = 0
                    if (hasDays) {
                        append(days).append('d')
                        components++
                    }
                    if (hasHours || (hasDays && (hasMinutes || hasSeconds))) {
                        if (components++ > 0) append(' ')
                        append(hours).append('h')
                    }
                    if (hasMinutes || (hasSeconds && (hasHours || hasDays))) {
                        if (components++ > 0) append(' ')
                        append(minutes).append('m')
                    }
                    if (hasSeconds) {
                        if (components++ > 0) append(' ')
                        when {
                            seconds != 0 || hasDays || hasHours || hasMinutes ->
                                appendFractional(seconds, nanoseconds, 9, "s", isoZeroes = false)
                            nanoseconds >= 1_000_000 ->
                                appendFractional(nanoseconds / 1_000_000, nanoseconds % 1_000_000, 6, "ms", isoZeroes = false)
                            nanoseconds >= 1_000 ->
                                appendFractional(nanoseconds / 1_000, nanoseconds % 1_000, 3, "us", isoZeroes = false)
                            else ->
                                append(nanoseconds).append("ns")
                        }
                    }
                    if (isNegative && components > 1) insert(1, '(').append(')')
                }
            }
        }
    }

    private fun StringBuilder.appendFractional(whole: Int, fractional: Int, fractionalSize: Int, unit: String, isoZeroes: Boolean) {
        append(whole)
        if (fractional != 0) {
            append('.')
            val fracString = fractional.toString().padStart(fractionalSize, '0')
            val nonZeroDigits = fracString.indexOfLast { it != '0' } + 1
            when {
                !isoZeroes && nonZeroDigits < 3 -> appendRange(fracString, 0, nonZeroDigits)
                else -> appendRange(fracString, 0, ((nonZeroDigits + 2) / 3) * 3)
            }
        }
        append(unit)
    }

    /**
     * Returns a string representation of this duration value expressed in the given [unit]
     * and formatted with the specified [decimals] number of digits after decimal point.
     *
     * Special cases:
     *  - an infinite duration is formatted as `"Infinity"` or `"-Infinity"` without a unit.
     *
     * @param decimals the number of digits after decimal point to show. The value must be non-negative.
     * No more than 12 decimals will be shown, even if a larger number is requested.
     *
     * @return the value of duration in the specified [unit] followed by that unit abbreviated name: `d`, `h`, `m`, `s`, `ms`, `us`, or `ns`.
     *
     * @throws IllegalArgumentException if [decimals] is less than zero.
     *
     * @sample samples.time.Durations.toStringDecimals
     */
    public fun toString(unit: DurationUnit, decimals: Int = 0): String {
        require(decimals >= 0) { "decimals must be not negative, but was $decimals" }
        val number = toDouble(unit)
        if (number.isInfinite()) return number.toString()
        return formatToExactDecimals(number, decimals.coerceAtMost(12)) + unit.shortName()
    }


    /**
     * Returns an ISO-8601 based string representation of this duration.
     *
     * The returned value is presented in the format `PThHmMs.fS`, where `h`, `m`, `s` are the integer components of this duration (see [toComponents])
     * and `f` is a fractional part of second. Depending on the roundness of the value the fractional part can be formatted with either
     * 0, 3, 6, or 9 decimal digits.
     *
     * The infinite duration is represented as `"PT9999999999999H"` which is larger than any possible finite duration in Kotlin.
     *
     * Negative durations are indicated with the sign `-` in the beginning of the returned string, for example, `"-PT5M30S"`.
     *
     * @sample samples.time.Durations.toIsoString
     */
    public fun toIsoString(): String = buildString {
        if (isNegative()) append('-')
        append("PT")
        this@Duration.absoluteValue.toComponents { hours, minutes, seconds, nanoseconds ->
            @Suppress("NAME_SHADOWING")
            var hours = hours
            if (isInfinite()) {
                // use large enough value instead of Long.MAX_VALUE
                hours = 9_999_999_999_999
            }
            val hasHours = hours != 0L
            val hasSeconds = seconds != 0 || nanoseconds != 0
            val hasMinutes = minutes != 0 || (hasSeconds && hasHours)
            if (hasHours) {
                append(hours).append('H')
            }
            if (hasMinutes) {
                append(minutes).append('M')
            }
            if (hasSeconds || (!hasHours && !hasMinutes)) {
                appendFractional(seconds, nanoseconds, 9, "S", isoZeroes = true)
            }
        }
    }

}

// constructing from number of units
// extension functions

/** Returns a [Duration] equal to this [Int] number of the specified [unit]. */
@SinceKotlin("1.6")
public fun Int.toDuration(unit: DurationUnit): Duration {
    return if (unit <= DurationUnit.SECONDS) {
        durationOfNanos(convertDurationUnitOverflow(this.toLong(), unit, DurationUnit.NANOSECONDS))
    } else
        toLong().toDuration(unit)
}

/** Returns a [Duration] equal to this [Long] number of the specified [unit]. */
@SinceKotlin("1.6")
public fun Long.toDuration(unit: DurationUnit): Duration {
    val maxNsInUnit = convertDurationUnitOverflow(MAX_NANOS, DurationUnit.NANOSECONDS, unit)
    if (this in -maxNsInUnit..maxNsInUnit) {
        return durationOfNanos(convertDurationUnitOverflow(this, unit, DurationUnit.NANOSECONDS))
    } else {
        val millis = convertDurationUnit(this, unit, DurationUnit.MILLISECONDS)
        return durationOfMillis(millis.coerceIn(-MAX_MILLIS, MAX_MILLIS))
    }
}

/**
 * Returns a [Duration] equal to this [Double] number of the specified [unit].
 *
 * Depending on its magnitude, the value is rounded to an integer number of nanoseconds or milliseconds.
 *
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.6")
public fun Double.toDuration(unit: DurationUnit): Duration {
    val valueInNs = convertDurationUnit(this, unit, DurationUnit.NANOSECONDS)
    require(!valueInNs.isNaN()) { "Duration value cannot be NaN." }
    val nanos = valueInNs.roundToLong()
    return if (nanos in -MAX_NANOS..MAX_NANOS) {
        durationOfNanos(nanos)
    } else {
        val millis = convertDurationUnit(this, unit, DurationUnit.MILLISECONDS).roundToLong()
        durationOfMillisNormalized(millis)
    }
}


/** Returns a duration whose value is the specified [duration] value multiplied by this number. */
@SinceKotlin("1.6")
@kotlin.internal.InlineOnly
public inline operator fun Int.times(duration: Duration): Duration = duration * this

/**
 * Returns a duration whose value is the specified [duration] value multiplied by this number.
 *
 * The operation may involve rounding when the result cannot be represented exactly with a [Double] number.
 *
 * @throws IllegalArgumentException if the operation results in a `NaN` value.
 */
@SinceKotlin("1.6")
@kotlin.internal.InlineOnly
public inline operator fun Double.times(duration: Duration): Duration = duration * this



private fun parseDuration(value: String, strictIso: Boolean, throwException: Boolean = true): Duration {
    val length = value.length
    if (length == 0) return throwExceptionOrInvalid(throwException, "The string is empty")
    var index = 0

    val firstChar = value[index]
    var isNegative = false
    if (firstChar == '-') {
        isNegative = true
        index++
    } else if (firstChar == '+') {
        index++
    }
    val hasSign = index > 0
    val result = when {
        length <= index -> return throwExceptionOrInvalid(throwException, "No components")
        value[index] == 'P' -> parseIsoStringFormat(value, index, length, throwException).onInvalid { return Duration.INVALID }
        strictIso -> return throwExceptionOrInvalid(throwException)
        value.regionMatches(index, INFINITY_STRING, 0, length = maxOf(length - index, INFINITY_STRING.length), ignoreCase = true) -> {
            Duration.INFINITE
        }
        else -> parseDefaultStringFormat(value, index, length, hasSign, throwException).onInvalid { return Duration.INVALID }
    }
    return if (isNegative) -result else result
}

@kotlin.internal.InlineOnly
private inline fun parseIsoStringFormat(
    value: String,
    startIndex: Int,
    length: Int,
    throwException: Boolean,
): Duration {
    var index = startIndex
    if (++index == length) return throwExceptionOrInvalid(throwException)
    var totalMillis = 0L
    var totalNanos = 0L
    var isTimeComponent = false
    var prevUnit = 'A'
    while (index < length) {
        val ch = value[index]
        if (ch == 'T') {
            if (isTimeComponent || ++index == length) return throwExceptionOrInvalid(throwException)
            isTimeComponent = true
            continue
        }
        val prevIndex = index
        val (longValue, nextIndex, sign) = value.parseLong(index)
        index = nextIndex
        if (index == length || index == prevIndex + if (ch == '-' || ch == '+') 1 else 0) return throwExceptionOrInvalid(throwException)
        var unit = value[index]
        if (unit == 'D') {
            if (isTimeComponent) return throwExceptionOrInvalid(throwException)
            totalMillis = longValue.multiplyWithoutOverflow(MILLIS_IN_DAY)
        } else {
            if (!isTimeComponent) return throwExceptionOrInvalid(throwException)
            totalMillis = totalMillis.addWithoutOverflow(
                longValue.multiplyWithoutOverflow(
                    when (unit) {
                        'H' -> MILLIS_IN_HOUR
                        'M' -> MILLIS_IN_MINUTE
                        'S', '.' -> MILLIS_IN_SECOND
                        else -> return throwExceptionOrInvalid(throwException, "Missing unit for value $longValue")
                    }
                )
            ).onInvalid { return throwExceptionOrInvalid(throwException) }
            if (unit == '.') {
                index++
                val prevIndex = index
                val (fractionValue, nextIndex) = value.parseFraction(index)
                index = nextIndex
                if (index == prevIndex || index == length || value[index] != 'S') return throwExceptionOrInvalid(throwException)
                totalNanos = sign * fractionValue.toNanos(DurationUnit.SECONDS)
                unit = 'S'
            }
            if (unit <= prevUnit) return throwExceptionOrInvalid(throwException, "Unexpected order of duration components")
            prevUnit = unit
        }
        index++
    }
    return totalMillis.toDuration(DurationUnit.MILLISECONDS) + totalNanos.toDuration(DurationUnit.NANOSECONDS)
}

@kotlin.internal.InlineOnly
private inline fun parseDefaultStringFormat(
    value: String,
    startIndex: Int,
    initialLength: Int,
    hasSign: Boolean,
    throwException: Boolean,
): Duration {
    var index = startIndex
    var length = initialLength
    var allowSpaces = !hasSign

    if (hasSign && value[index] == '(' && value.last() == ')') {
        allowSpaces = true
        index++
        length--
        if (index == length) return throwExceptionOrInvalid(throwException, "No components")
    }

    var totalMillis = 0L
    var totalNanos = 0L
    var prevUnit: DurationUnit? = null
    var isFirstComponent = true

    while (index < length) {
        if (!isFirstComponent && allowSpaces) {
            index = value.skipWhile(index) { it == ' ' }
        }
        isFirstComponent = false

        val integralStartIndex = index
        val (integralValue, afterIntegerIndex, sign) = value.parseLong(index, withSign = false, overflowLimit = Long.MAX_VALUE)
        if (afterIntegerIndex == integralStartIndex || afterIntegerIndex == length) {
            return throwExceptionOrInvalid(throwException)
        }
        if (sign == -1) return throwExceptionOrInvalid(throwException)
        index = afterIntegerIndex

        val hasFractionalPart = value[index] == '.'
        val fractionValue = if (hasFractionalPart) {
            index++
            val fractionStartIndex = index
            val (fraction, afterFractionIndex) = value.parseFraction(index)
            if (afterFractionIndex == fractionStartIndex || afterFractionIndex == length) {
                return throwExceptionOrInvalid(throwException)
            }
            index = afterFractionIndex
            fraction
        } else 0L

        val unit = value.durationUnitByShortNameOrNull(index) ?: return throwExceptionOrInvalid(throwException)
        if (prevUnit != null && prevUnit <= unit) {
            return throwExceptionOrInvalid(throwException, "Unexpected order of duration components")
        }
        prevUnit = unit

        when (unit) {
            DurationUnit.MICROSECONDS -> {
                totalMillis += integralValue / MICROS_IN_MILLIS
                totalNanos += (integralValue % MICROS_IN_MILLIS) * NANOS_IN_MICROS
            }
            DurationUnit.NANOSECONDS -> {
                totalMillis += integralValue / NANOS_IN_MILLIS
                totalNanos += integralValue % NANOS_IN_MILLIS
            }
            else -> {
                val multiplier = unit.millisMultiplier
                totalMillis = totalMillis.addWithoutOverflow(integralValue.multiplyWithoutOverflow(multiplier))
            }
        }

        totalNanos += fractionValue.toNanos(unit)
        index += unit.length
        if (hasFractionalPart && index < length) {
            return throwExceptionOrInvalid(throwException, "Fractional component must be last")
        }
    }

    return totalMillis.toDuration(DurationUnit.MILLISECONDS) + totalNanos.toDuration(DurationUnit.NANOSECONDS)
}

@kotlin.internal.InlineOnly
private inline fun willMultiplyOverflow(a: Long, b: Long): Boolean = when {
    a == 0L -> false
    a > 0 -> a > MAX_MILLIS / b
    else -> a < -MAX_MILLIS / b
}

@kotlin.internal.InlineOnly
private inline fun Long.multiplyWithoutOverflow(other: Long): Long = when {
    willMultiplyOverflow(this, other) -> if (this > 0) MAX_MILLIS else -MAX_MILLIS
    else -> this * other
}

@kotlin.internal.InlineOnly
private inline fun willAddOverflow(a: Long, b: Long): Boolean = when {
    a > 0 && b > 0 -> a > MAX_MILLIS - b
    a < 0 && b < 0 -> a < -MAX_MILLIS - b
    else -> false
}

@kotlin.internal.InlineOnly
private inline fun Long.addWithoutOverflow(other: Long): Long = when {
    this == -MAX_MILLIS && other == MAX_MILLIS || this == MAX_MILLIS && other == -MAX_MILLIS -> Duration.INVALID_RAW_VALUE
    this == MAX_MILLIS || other == MAX_MILLIS -> MAX_MILLIS
    this == -MAX_MILLIS || other == -MAX_MILLIS -> -MAX_MILLIS
    willAddOverflow(this, other) -> if (this > 0) MAX_MILLIS else -MAX_MILLIS
    else -> this + other
}

private data class NumericParseData(val value: Long, val index: Int, val sign: Int = 1)

@kotlin.internal.InlineOnly
private inline fun String.parseLong(startIndex: Int, withSign: Boolean = true, overflowLimit: Long = MAX_MILLIS): NumericParseData {
    var sign = 1
    var index = startIndex
    if (withSign) {
        val firstChar = this[index]
        if (firstChar == '-') {
            sign = -1
            index++
        } else if (firstChar == '+') {
            index++
        }
    }
    while (index < length && this[index] == '0') index++
    var result = 0L
    val overflowThreshold = overflowLimit / 10
    val lastDigitMax = overflowLimit % 10
    while (index < length) {
        val ch = this[index]
        if (ch !in '0'..'9') break
        val digit = ch - '0'
        if (result > overflowThreshold || (result == overflowThreshold && digit > lastDigitMax)) {
            while (index < length && this[index] in '0'..'9') index++
            return NumericParseData(overflowLimit * sign, index, if (!withSign) -1 else sign)
        }
        result = result * 10 + digit
        index++
    }
    return NumericParseData(result * sign, index, sign)
}

@kotlin.internal.InlineOnly
private inline fun String.parseFraction(startIndex: Int): NumericParseData {
    var result = 0L
    var index = startIndex
    var multiplier = 100_000_000_000_000L
    while (index < length && multiplier > 0) {
        val ch = this[index]
        if (ch !in '0'..'9') break
        val digit = ch - '0'
        result += digit * multiplier
        multiplier /= 10
        index++
    }
    while (index < length && this[index] in '0'..'9') index++
    return NumericParseData(result, index)
}

@kotlin.internal.InlineOnly
private inline fun Long.toNanos(unit: DurationUnit): Long = (this * unit.fractionMultiplier).roundToLong()

@kotlin.internal.InlineOnly
private inline fun throwExceptionOrInvalid(throwException: Boolean, message: String = ""): Duration {
    if (throwException) throw IllegalArgumentException(message)
    return Duration.INVALID
}

private inline fun Duration.onInvalid(block: () -> Nothing): Duration {
    return if (this == Duration.INVALID) block() else this
}

private inline fun Long.onInvalid(block: () -> Nothing): Long {
    return if (this == Duration.INVALID_RAW_VALUE) block() else this
}

private inline fun String.skipWhile(startIndex: Int, predicate: (Char) -> Boolean): Int {
    var i = startIndex
    while (i < length && predicate(this[i])) i++
    return i
}




// The ranges are chosen so that they are:
// - symmetric relative to zero: this greatly simplifies operations with sign, e.g. unaryMinus and minus.
// - non-overlapping, but adjacent: the first value that doesn't fit in nanos range, can be exactly represented in millis.

internal const val NANOS_IN_MILLIS = 1_000_000
internal const val MICROS_IN_MILLIS = 1_000L
internal const val NANOS_IN_MICROS = 1_000L
// maximum number duration can store in nanosecond range
internal const val MAX_NANOS = Long.MAX_VALUE / 2 / NANOS_IN_MILLIS * NANOS_IN_MILLIS - 1 // ends in ..._999_999
// maximum number duration can store in millisecond range, also encodes an infinite value
internal const val MAX_MILLIS = Long.MAX_VALUE / 2
// MAX_NANOS expressed in milliseconds
private const val MAX_NANOS_IN_MILLIS = MAX_NANOS / NANOS_IN_MILLIS

internal const val MILLIS_IN_SECOND = 1000L
internal const val MILLIS_IN_MINUTE = MILLIS_IN_SECOND * 60L
internal const val MILLIS_IN_HOUR = MILLIS_IN_MINUTE * 60L
internal const val MILLIS_IN_DAY = MILLIS_IN_HOUR * 24L

private const val INFINITY_STRING = "Infinity"

private fun nanosToMillis(nanos: Long): Long = nanos / NANOS_IN_MILLIS
private fun millisToNanos(millis: Long): Long = millis * NANOS_IN_MILLIS

private fun durationOfNanos(normalNanos: Long) = Duration(normalNanos shl 1)
private fun durationOfMillis(normalMillis: Long) = Duration((normalMillis shl 1) + 1)
private fun durationOf(normalValue: Long, unitDiscriminator: Int) = Duration((normalValue shl 1) + unitDiscriminator)
private fun durationOfNanosNormalized(nanos: Long) =
    if (nanos in -MAX_NANOS..MAX_NANOS) {
        durationOfNanos(nanos)
    } else {
        durationOfMillis(nanosToMillis(nanos))
    }

private fun durationOfMillisNormalized(millis: Long) =
    if (millis in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS) {
        durationOfNanos(millisToNanos(millis))
    } else {
        durationOfMillis(millis.coerceIn(-MAX_MILLIS, MAX_MILLIS))
    }

internal expect val durationAssertionsEnabled: Boolean

internal expect fun formatToExactDecimals(value: Double, decimals: Int): String
