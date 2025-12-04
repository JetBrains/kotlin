/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
public value class Duration
// A temporary workaround for KT-81995, the constructor has to be private once the issue is resolved.
@Deprecated("Don't call this constructor directly.", level = DeprecationLevel.ERROR)
internal constructor(private val rawValue: Long) :
    Comparable<Duration> {

    private val value: Long get() = rawValue shr 1
    private inline val unitDiscriminator: Int get() = rawValue.toInt() and 1
    private fun isInNanos() = unitDiscriminator == 0
    private fun isInMillis() = unitDiscriminator == 1
    private val storageUnit get() = if (isInNanos()) DurationUnit.NANOSECONDS else DurationUnit.MILLISECONDS

    public companion object {
        @Suppress("DEPRECATION_ERROR") // A temporary workaround for KT-81995.
        internal fun fromRawValue(rawValue: Long): Duration = Duration(rawValue).apply {
            if (durationAssertionsEnabled) {
                if (isInNanos()) {
                    if (value !in -MAX_NANOS..MAX_NANOS) throw AssertionError("$value ns is out of nanoseconds range")
                } else {
                    if (!value.isFiniteMillis() && !value.isInfiniteMillis()) throw AssertionError("$value ms is out of milliseconds range")
                    if (value in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS) throw AssertionError("$value ms is denormalized")
                }
            }
        }

        /** The duration equal to exactly 0 seconds. */
        @Suppress("DEPRECATION_ERROR") // A temporary workaround for KT-81995.
        public val ZERO: Duration = Duration(0L)

        /** The duration whose value is positive infinity. It is useful for representing timeouts that should never expire. */
        public val INFINITE: Duration = durationOfMillis(MAX_MILLIS)
        internal val NEG_INFINITE: Duration = durationOfMillis(-MAX_MILLIS)

        internal const val INVALID_RAW_VALUE = 0x7FFFFFFFFFFFC0DE
        @Suppress("DEPRECATION_ERROR") // A temporary workaround for KT-81995.
        internal val INVALID = Duration(INVALID_RAW_VALUE)

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
            parseDuration(value, strictIso = false).apply { check(this != INVALID) { "invariant failed" } }
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
            parseDuration(value, strictIso = true).apply { check(this != INVALID) { "invariant failed" } }
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
            parseDuration(value, strictIso = false, throwException = false).onInvalid { null }

        /**
         * Parses a string that represents a duration in restricted ISO-8601 composite representation
         * and returns the parsed [Duration] value or `null` if the string doesn't represent a duration in the format
         * acceptable by [parseIsoString].
         *
         * @sample samples.time.Durations.parseIsoString
         */
        public fun parseIsoStringOrNull(value: String): Duration? =
            parseDuration(value, strictIso = true, throwException = false).onInvalid { null }
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
    public operator fun plus(other: Duration): Duration = when {
        unitDiscriminator == other.unitDiscriminator -> when {
            isInNanos() -> durationOfNanosNormalized(value + other.value)
            else -> value.addMillisWithoutOverflow(other.value).let {
                when {
                    it == INVALID_RAW_VALUE -> throw IllegalArgumentException("Summing infinite durations of different signs yields an undefined result.")
                    it.isInfiniteMillis() -> durationOfMillis(it)
                    else -> durationOfMillisNormalized(it)
                }
            }
        }
        this.isInMillis() -> addValuesMixedRanges(value, other.value)
        else -> addValuesMixedRanges(other.value, value)
    }

    private fun addValuesMixedRanges(thisMillis: Long, otherNanos: Long): Duration {
        val otherMillis = nanosToMillis(otherNanos)
        // resultMillis will never be INVALID because otherMillis is always finite value
        // (otherNanos comes from nanos range which excludes infinities, so otherMillis is also finite)
        val resultMillis = thisMillis.addMillisWithoutOverflow(otherMillis)
        return if (resultMillis in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS) {
            val otherNanoRemainder = otherNanos - millisToNanos(otherMillis)
            durationOfNanos(millisToNanos(resultMillis) + otherNanoRemainder)
        } else {
            durationOfMillis(resultMillis)
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
    return when {
        this in -maxNsInUnit..maxNsInUnit -> durationOfNanos(convertDurationUnitOverflow(this, unit, DurationUnit.NANOSECONDS))
        unit >= DurationUnit.MILLISECONDS -> durationOfMillis(
            this.sign * convertDurationUnitToMilliseconds(
                abs(this.coerceAtLeast(Long.MIN_VALUE + 1)),
                unit
            )
        )
        else -> durationOfMillis(convertDurationUnit(this, unit, DurationUnit.MILLISECONDS).coerceIn(-MAX_MILLIS, MAX_MILLIS))
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


/**
 * Parses a duration string in either ISO-8601 or default format.
 *
 * @param value the string to parse
 * @param strictIso if `true`, only accepts an ISO-8601 format; if `false`, accepts both ISO and default formats
 * @param throwException if `true`, throws [IllegalArgumentException] on parse error; if `false`, returns [Duration.INVALID]
 * @return parsed [Duration] or [Duration.INVALID] on error when throwException is `false`
 */
private fun parseDuration(value: String, strictIso: Boolean, throwException: Boolean = true): Duration {
    if (value.isEmpty()) return handleError(throwException, "The string is empty")
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
        value.length <= index -> return handleError(throwException, "No components")
        value[index] == 'P' -> parseIsoStringFormat(value, index + 1, throwException)
        strictIso -> return handleError(throwException)
        value.regionMatches(index, INFINITY_STRING, 0, length = maxOf(value.length - index, INFINITY_STRING.length), ignoreCase = true) -> {
            Duration.INFINITE
        }
        else -> parseDefaultStringFormat(value, index, hasSign, throwException)
    }
    return if (isNegative && result != Duration.INVALID) -result else result
}

/**
 * Parses ISO-8601 duration format (e.g., `"PT1H30M45S"`).
 *
 * @param value the full input string
 * @param startIndex index after `'P'` prefix
 * @param throwException if `true`, throws an exception on error, otherwise returns [Duration.INVALID]
 * @return parsed Duration or [Duration.INVALID] on error
 */
private fun parseIsoStringFormat(
    value: String,
    startIndex: Int,
    throwException: Boolean,
): Duration {
    var index = startIndex
    if (index == value.length) return handleError(throwException)

    var totalMillis = 0L
    var totalNanos = 0L
    var isTimeComponent = false
    var prevUnit: DurationUnit? = null

    /*
     * ISO format consists of multiple repeated parts, all having the following format:
     * <sign><long value>[.<fraction>]<unit>.
     *
     * On every iteration, we consequently consume a chunk of the input string matching this format.
     * If there are any unexpected chars or other inconsistencies, an error will be reported.
     */
    while (index < value.length) {
        val ch = value[index]
        if (ch == 'T') {
            if (isTimeComponent || ++index == value.length) return handleError(throwException)
            isTimeComponent = true
            continue
        }

        val longStartIndex = index
        val sign: Int
        // In case of overflow, LongParser.iso.parse will return MAX_MILLIS, which is ultimately
        // equivalent to INFINITE, we don't need to handle overflow specially here.
        val longValue = LongParser.iso.parse(value, index) { longEndIndex, localSign, _ ->
            index = longEndIndex
            // A numerical value should not be empty, and it has to be followed by a unit (i.e., it cannot terminate a string)
            if (index == value.length || index == longStartIndex + if (ch == '-' || ch == '+') 1 else 0) return handleError(throwException)
            sign = localSign
        }

        if (value[index] == '.') {
            index++
            val fractionValue = FractionalParser.parse(value, index) { fractionEndIndex ->
                // A fraction has to be non-empty, it has to be followed by a unit, and the only unit supporting
                // fractional parts is *S*econds.
                if (fractionEndIndex == index || fractionEndIndex == value.length || value[fractionEndIndex] != 'S') {
                    return handleError(throwException)
                }
                index = fractionEndIndex
            }
            totalNanos = sign * fractionValue.fractionDigitsToNanos(DurationUnit.SECONDS)
        }

        val unit = value.isoDurationUnitByShortNameOrNull(index)
            ?: return handleError(throwException, "Unknown duration unit short name: ${value[index]}")
        if (prevUnit != null && prevUnit <= unit) return handleError(throwException, "Unexpected order of duration components")
        prevUnit = unit

        if (unit == DurationUnit.DAYS) {
            if (isTimeComponent) return handleError(throwException)
            totalMillis = sign * convertDurationUnitToMilliseconds(longValue, unit)
        } else {
            if (!isTimeComponent) return handleError(throwException)
            totalMillis = totalMillis.addMillisWithoutOverflow(sign * convertDurationUnitToMilliseconds(longValue, unit))
                .also { if (it == Duration.INVALID_RAW_VALUE) return handleError(throwException) }
        }

        index++
    }

    return totalMillis.toDuration(DurationUnit.MILLISECONDS) + totalNanos.toDuration(DurationUnit.NANOSECONDS)
}

/**
 * Parses default duration format (e.g., `"1h 30m"`, `"45s"`, `"500ms"`).
 * Note: While `"Infinity"` is a part of the default format specification, this function
 * does not handle it - infinite values are handled separately before calling this method.
 * @param value the input string
 * @param startIndex starting position for parsing
 * @param hasSign whether the duration had a leading sign
 * @param throwException if `true`, throws an exception on error, otherwise returns [Duration.INVALID]
 * @return parsed [Duration] or [Duration.INVALID] on error
 */
private fun parseDefaultStringFormat(
    value: String,
    startIndex: Int,
    hasSign: Boolean,
    throwException: Boolean,
): Duration {
    var index = startIndex
    var length = value.length
    var allowSpaces = !hasSign

    if (hasSign && value[index] == '(' && value[length - 1] == ')') {
        allowSpaces = true
        index++
        length--
        if (index == length) return handleError(throwException, "No components")
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

        val longStartIndex = index
        val longValue = LongParser.default.parse(value, index) { longEndIndex, _, hasOverflow ->
            // A numeric value has to be non-empty, and it has to be followed by a unit (i.e., it cannot be the last in string)
            if (longEndIndex == longStartIndex || longEndIndex == length || hasOverflow) return handleError(throwException)
            index = longEndIndex
        }

        val hasFractionalPart = value[index] == '.'
        val fractionStartIndex: Int
        val fractionValue: Long
        if (hasFractionalPart) {
            fractionStartIndex = index
            index++
            fractionValue = FractionalParser.parse(value, index) { fractionEndIndex ->
                // Fraction has to be non-empty, and it cannot terminate a string
                if (fractionEndIndex == index || fractionEndIndex == length) return handleError(throwException)
                index = fractionEndIndex
            }
        } else {
            fractionStartIndex = -1
            fractionValue = 0L
        }

        val unit = value.defaultDurationUnitByShortNameOrNull(index)
            ?: return handleError(throwException, "Unknown duration unit short name: ${value[index]}")
        if (prevUnit != null && prevUnit <= unit) return handleError(throwException, "Unexpected order of duration components")
        prevUnit = unit

        when (unit) {
            DurationUnit.MICROSECONDS -> {
                // We extract the millisecond portion from microseconds and transfer it to totalMillis.
                // Since totalMillis is at most MAX_MILLIS (Long.MAX_VALUE / 2) and the added value is at most Long.MAX_VALUE / 1_000,
                // their sum (Long.MAX_VALUE / 2 + Long.MAX_VALUE / 1_000) will never overflow.
                totalMillis += longValue / MICROS_IN_MILLIS
                // If it's possible to represent milliseconds as nanoseconds, we convert the last 3 digits of microseconds to nanoseconds.
                if (totalMillis <= MAX_NANOS / NANOS_IN_MILLIS) {
                    // Value is at most 999_000
                    totalNanos = (longValue % MICROS_IN_MILLIS) * NANOS_IN_MICROS
                }
            }
            DurationUnit.NANOSECONDS -> {
                // We extract the millisecond portion from nanoseconds and transfer it to totalMillis.
                // Since totalMillis is at most MAX_MILLIS (Long.MAX_VALUE / 2) and the added value is at most Long.MAX_VALUE / 1_000_000,
                // their sum (Long.MAX_VALUE / 2 + Long.MAX_VALUE / 1_000_000) will never overflow.
                totalMillis += longValue / NANOS_IN_MILLIS
                // Value is at most 999_000 + 999_999 = 1_998_999
                totalNanos += longValue % NANOS_IN_MILLIS
            }
            else -> {
                // When other time units are greater than or equal to milliseconds,
                // we convert them to milliseconds, add them to totalMillis, and preserve any overflow.
                totalMillis = totalMillis.addMillisWithoutOverflow(convertDurationUnitToMilliseconds(longValue, unit))
            }
        }

        index += unit.shortNameLength

        if (hasFractionalPart) {
            if (index < length) {
                return handleError(throwException, "Fractional component must be last")
            }

            // Since totalNanos is at most 1_998_999, and the added value is at most 10^15, their sum will never overflow.
            // We use comparison with MINUTES because for days, hours, and minutes, the nanosecond
            // is not expressed as a finite, non-repeating decimal, requiring fallback parsing for long fraction part.
            totalNanos += if (unit >= DurationUnit.MINUTES && index - fractionStartIndex > FRACTION_LIMIT)
                value.parseFractionFallback(fractionStartIndex, index - unit.shortNameLength, unit)
            else
                fractionValue.fractionDigitsToNanos(unit)
        }
    }

    return totalMillis.toDuration(DurationUnit.MILLISECONDS) + totalNanos.toDuration(DurationUnit.NANOSECONDS)
}

/**
 * Parser for long integer values with overflow detection and optional sign handling.
 *
 * This class provides efficient parsing of long values from strings with built-in
 * overflow detection based on a configurable limit. It supports optional sign
 * parsing for formats that require it (e.g., ISO 8601).
 *
 * When overflow occurs, the parser returns the maximum allowed value ([overflowLimit])
 * and reports the overflow condition through the callback parameter.
 *
 * @property overflowLimit The maximum value that can be parsed without overflow
 * @property allowSign Whether to parse the optional `'+'` or `'-'` sign at the beginning
 */
internal class LongParser private constructor(private val overflowLimit: Long, private val allowSign: Boolean) {

    // Pre-calculated threshold (overflowLimit / 10) for early overflow detection
    private val overflowThreshold = overflowLimit / 10

    // Maximum allowed last digit (overflowLimit % 10) when at the overflow threshold
    private val lastDigitMax = overflowLimit % 10

    /**
     * Parses a long integer from the string starting at the specified index.
     *
     * @param value The string to parse
     * @param startIndex The index to start parsing from
     * @param callback Invoked with (endIndex, sign, hasOverflow) when parsing completes
     * @return The parsed value, clamped to [overflowLimit] if overflow occurred
     */
    inline fun parse(value: String, startIndex: Int, callback: (endIndex: Int, sign: Int, hasOverflow: Boolean) -> Unit): Long {
        contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
        var sign = 1
        var index = startIndex
        if (allowSign) {
            val firstChar = value[index]
            if (firstChar == '-') {
                sign = -1
                index++
            } else if (firstChar == '+') {
                index++
            }
        }
        index = value.skipWhile(index) { it == '0' }
        var result = 0L
        while (index < value.length) {
            val ch = value[index]
            if (ch !in '0'..'9') break
            val digit = ch - '0'
            if (result > overflowThreshold || (result == overflowThreshold && digit > lastDigitMax)) {
                index = value.skipWhile(index) { it in '0'..'9' }
                callback(index, sign, true)
                return overflowLimit
            }
            result = result.multiplyBy10() + digit
            index++
        }
        callback(index, sign, false)
        return result
    }

    companion object {
        val iso = LongParser(MAX_MILLIS, allowSign = true)
        val default = LongParser(Long.MAX_VALUE, allowSign = false)
    }
}

/**
 * Parser for fractional parts of duration values.
 *
 * This object efficiently parses up to 15 decimal digits from a string,
 * converting them to nanoseconds. It handles the fractional component
 * that appears after a decimal point in duration strings (e.g., the `".5"` in `"1.5s"`).
 *
 * The parser splits the 15-digit precision into two parts for efficiency (on JS):
 * - High precision: first 9 digits
 * - Low precision: remaining 6 digits
 */
internal object FractionalParser {

    /**
     * Parses a sequence of decimal digits as a decimal fractional part and returns it as an integer
     * scaled to 15 decimal places, ignoring all trailing digits after the first 15.
     *
     * @param value The string to parse
     * @param startIndex The index to start parsing from
     * @param callback Invoked with the end index after parsing
     * @return The fraction scaled to 15 decimal places (suitable for nanosecond conversion)
     */
    inline fun parse(value: String, startIndex: Int, callback: (endIndex: Int) -> Unit): Long {
        var index = startIndex
        val highPrecisionDigits = value.parseDigits(index, FRACTION_LIMIT - 9) { index = it }
        val lowPrecisionDigits = value.parseDigits(index, 9) { index = it }
        index = value.skipWhile(index) { it in '0'..'9' }
        callback(index)
        return highPrecisionDigits.toLong() * 1_000_000_000 + lowPrecisionDigits
    }

    private inline fun String.parseDigits(startIndex: Int, maxDigits: Int, callback: (endIndex: Int) -> Unit): Int {
        var index = startIndex
        val endIndex = minOf(index + maxDigits, length)
        var result = 0
        while (index < endIndex) {
            val ch = this[index]
            if (ch !in '0'..'9') break
            result = result.multiplyBy10() + (ch - '0')
            index++
        }
        repeat(maxDigits - (index - startIndex)) {
            result = result.multiplyBy10()
        }
        callback(index)
        return result
    }
}

/**
 * Adds two Long values representing milliseconds without overflow.
 *
 * Handles infinite values and ensures the result stays within the valid range.
 *
 * @param other the value in milliseconds to add
 * @return the sum in milliseconds clamped to `[-[MAX_MILLIS], [MAX_MILLIS]]` range, or [Duration.INVALID_RAW_VALUE] for invalid operations
 */
private fun Long.addMillisWithoutOverflow(other: Long): Long = when {
    isInfiniteMillis() -> if (other.isFiniteMillis() || sameSign(this, other)) this else Duration.INVALID_RAW_VALUE
    other.isInfiniteMillis() -> other
    else -> (this + other).coerceIn(-MAX_MILLIS, MAX_MILLIS)
}

/**
 * Checks if this Long value represents an infinite duration in milliseconds.
 *
 * This corresponds to the internal representation of [Duration.INFINITE] and [Duration.NEG_INFINITE].
 *
 * @return true if this value equals [MAX_MILLIS] or -[MAX_MILLIS], false otherwise
 */
@kotlin.internal.InlineOnly
private inline fun Long.isInfiniteMillis(): Boolean = this == MAX_MILLIS || this == -MAX_MILLIS

/**
 * Checks if this Long value represents a finite duration in milliseconds.
 *
 * @return true if this value is in the range (-[MAX_MILLIS], [MAX_MILLIS]) (exclusive bounds), false otherwise
 */
@kotlin.internal.InlineOnly
private inline fun Long.isFiniteMillis(): Boolean = -MAX_MILLIS < this && this < MAX_MILLIS

/**
 * Checks if two Long values have the same sign.
 *
 * @param a the first value
 * @param b the second value
 * @return `true` if both values have the same sign (both positive or both negative), `false` otherwise
 */
@kotlin.internal.InlineOnly
private inline fun sameSign(a: Long, b: Long): Boolean = a xor b >= 0L

/**
 * Fallback for parsing fractions with more than 15 digits using Double parsing.
 *
 * @param startIndex start of the fraction substring (including decimal point)
 * @param endIndex end of the fraction substring
 * @param unit the duration unit of the whole part before the fraction
 * @return nanoseconds representing the fractional part
 */
private fun String.parseFractionFallback(startIndex: Int, endIndex: Int, unit: DurationUnit): Long =
    (substring(startIndex, endIndex).toDouble() * unit.fallbackFractionMultiplier).roundToLong()

/**
 * Converts fraction digits (scaled to 15 decimal places) to nanoseconds for the given unit.
 *
 * @param unit the duration unit of the whole part before the fraction
 * @return nanoseconds representing the fractional part
 */
private fun Long.fractionDigitsToNanos(unit: DurationUnit): Long = (this * unit.fractionMultiplier).roundToLong()

/**
 * Handles parsing errors based on the [throwException] flag.
 *
 * @param throwException if true, throws [IllegalArgumentException]; if false, returns [Duration.INVALID]
 * @param message optional error message for the exception
 * @return [Duration.INVALID] (only when [throwException] is `false`)
 * @throws IllegalArgumentException when [throwException] is `true`
 */
@kotlin.internal.InlineOnly
private inline fun handleError(throwException: Boolean, message: String = ""): Duration {
    if (throwException) throw IllegalArgumentException(message)
    return Duration.INVALID
}

/**
 * Executes the given block if [this] is [Duration.INVALID], otherwise returns this Duration.
 *
 * @param block lambda to execute if Duration is [Duration.INVALID]
 * @return [this] if valid, or the result of the block if [Duration.INVALID]
 */
private inline fun Duration.onInvalid(block: () -> Duration?): Duration? = if (this == Duration.INVALID) block() else this

/**
 * Parses a duration unit from its default format short name at the given position.
 *
 * Recognizes lowercase unit abbreviations:
 * - Single character: d (days), h (hours), m (minutes), s (seconds)
 * - Two characters: ms (milliseconds), us (microseconds), ns (nanoseconds)
 *
 * @param start the index in the string where the unit name starts
 * @return the corresponding [DurationUnit] or null if no valid unit is found
 */
private fun String.defaultDurationUnitByShortNameOrNull(start: Int): DurationUnit? {
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

/**
 * Parses a duration unit from its ISO 8601 short name at the given position.
 *
 * Recognizes: `D` (days), `H` (hours), `M` (minutes), `S` (seconds).
 *
 * @param start the index in the string where the unit name starts
 * @return the corresponding [DurationUnit] or null if no valid unit is found
 */
private fun String.isoDurationUnitByShortNameOrNull(start: Int): DurationUnit? =
    when (this[start]) {
        'D' -> DurationUnit.DAYS
        'H' -> DurationUnit.HOURS
        'M' -> DurationUnit.MINUTES
        'S' -> DurationUnit.SECONDS
        else -> null
    }

/**
 * Multiplier to convert a 15-digit fraction to nanoseconds for this unit.
 * Used for efficient fraction parsing when the fraction has 15 or fewer digits.
 *
 * These values are calculated as: nanoseconds_in_unit / 10^15
 * Examples:
 * - SECONDS: 1_000_000_000 ns / 10^15 = 0.000001
 * - DAYS: 86_400_000_000_000 ns / 10^15 = 0.0864
 * This allows direct multiplication: fraction_value * multiplier = nanoseconds
 */
@Suppress("REDUNDANT_ELSE_IN_WHEN")
private val DurationUnit.fractionMultiplier: Double
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

/**
 * Multiplier to convert a Double fraction (0.0 to 1.0) to nanoseconds for this unit.
 * Used as fallback for fractions with more than 15 digits that require Double parsing.
 *
 * These values represent the total number of nanoseconds in one unit:
 * - MINUTES: 60 seconds * 1_000_000_000 ns/s = 60_000_000_000 ns
 * - HOURS: 3600 seconds * 1_000_000_000 ns/s = 3_600_000_000_000 ns
 * - DAYS: 86400 seconds * 1_000_000_000 ns/s = 86_400_000_000_000 ns
 * Smaller units don't need fallback as they fit within 15-digit precision.
 */
private val DurationUnit.fallbackFractionMultiplier: Long
    get() = when (this) {
        DurationUnit.MINUTES -> 60_000_000_000L
        DurationUnit.HOURS -> 3_600_000_000_000L
        DurationUnit.DAYS -> 86_400_000_000_000L
        else -> error("Invalid unit: $this for fallback fraction multiplier")
    }

/**
 * The length of this unit's short name in characters.
 * Returns 2 for two-character units (ms, us, ns) and 1 for single-character units (d, h, m, s).
 */
private val DurationUnit.shortNameLength: Int
    get() = when (this) {
        DurationUnit.MILLISECONDS, DurationUnit.MICROSECONDS, DurationUnit.NANOSECONDS -> 2
        else -> 1
    }

/**
 * Multiplies this Long value by 10 using bit-shift operations for improved performance.
 *
 * The multiplication is performed as:
 * `this * 10 = this * (8 + 2) = (this * 8) + (this * 2) = (this * 2^3) + (this * 2^1) = (this shl 3) + (this shl 1)`
 *
 * This optimization replaces a multiplication operation with two bit-shifts and an addition, which is more efficient.
 *
 * @return this value multiplied by 10
 */
@kotlin.internal.InlineOnly
private inline fun Long.multiplyBy10(): Long = (this shl 3) + (this shl 1)

/**
 * Read [Long.multiplyBy10] KDoc for details.
 */
@kotlin.internal.InlineOnly
private inline fun Int.multiplyBy10(): Int = (this shl 3) + (this shl 1)


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

private const val FRACTION_LIMIT = 15

private fun nanosToMillis(nanos: Long): Long = nanos / NANOS_IN_MILLIS
private fun millisToNanos(millis: Long): Long = millis * NANOS_IN_MILLIS

private fun durationOfNanos(normalNanos: Long) = Duration.fromRawValue(normalNanos shl 1)
private fun durationOfMillis(normalMillis: Long) = Duration.fromRawValue((normalMillis shl 1) + 1)
private fun durationOf(normalValue: Long, unitDiscriminator: Int) = Duration.fromRawValue((normalValue shl 1) + unitDiscriminator)
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
