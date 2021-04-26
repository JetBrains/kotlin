/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.contracts.*
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Represents the amount of time one instant of time is away from another instant.
 *
 * A negative duration is possible in a situation when the second instant is earlier than the first one.
 * An infinite duration value [Duration.INFINITE] can be used to represent infinite timeouts.
 *
 * The type can store duration values up to ±146 years with nanosecond precision,
 * and up to ±146 million years with millisecond precision.
 *
 * To construct a duration, use either the extension function [toDuration] available on [Int], [Long], and [Double] numeric types,
 * or the `Duration` companion object functions [Duration.hours], [Duration.minutes], [Duration.seconds], and so on,
 * taking [Int], [Long], or [Double] numbers as parameters.
 *
 * To get the value of this duration expressed in a particular [duration units][DurationUnit]
 * use the functions [toInt], [toLong], and [toDouble]
 * or the properties [inWholeHours], [inWholeMinutes], [inWholeSeconds], [inWholeNanoseconds], and so on.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@JvmInline
public value class Duration internal constructor(private val rawValue: Long) : Comparable<Duration> {

    private val value: Long get() = rawValue shr 1
    private inline val unitDiscriminator: Int get() = rawValue.toInt() and 1
    private fun isInNanos() = unitDiscriminator == 0
    private fun isInMillis() = unitDiscriminator == 1
    private val storageUnit get() = if (isInNanos()) DurationUnit.NANOSECONDS else DurationUnit.MILLISECONDS

    init {
        // TODO: disable assertions in final version
        if (isInNanos()) {
            if (value !in -MAX_NANOS..MAX_NANOS) throw AssertionError("$value ns is out of nanoseconds range")
        } else {
            if (value !in -MAX_MILLIS..MAX_MILLIS) throw AssertionError("$value ms is out of milliseconds range")
            if (value in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS) throw AssertionError("$value ms is denormalized")
        }
    }

    companion object {
        /** The duration equal to exactly 0 seconds. */
        public val ZERO: Duration = Duration(0L)

        /** The duration whose value is positive infinity. It is useful for representing timeouts that should never expire. */
        public val INFINITE: Duration = durationOfMillis(MAX_MILLIS)
        internal val NEG_INFINITE: Duration = durationOfMillis(-MAX_MILLIS)

        /** Converts the given time duration [value] expressed in the specified [sourceUnit] into the specified [targetUnit]. */
        public fun convert(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double =
            convertDurationUnit(value, sourceUnit, targetUnit)

        /** Returns a [Duration] representing the specified [value] number of nanoseconds. */
        @SinceKotlin("1.5")
        public fun nanoseconds(value: Int): Duration = value.toDuration(DurationUnit.NANOSECONDS)

        /** Returns a [Duration] representing the specified [value] number of nanoseconds. */
        @SinceKotlin("1.5")
        public fun nanoseconds(value: Long): Duration = value.toDuration(DurationUnit.NANOSECONDS)

        /**
         * Returns a [Duration] representing the specified [value] number of nanoseconds.
         *
         * @throws IllegalArgumentException if the provided `Double` [value] is `NaN`.
         */
        @SinceKotlin("1.5")
        public fun nanoseconds(value: Double): Duration = value.toDuration(DurationUnit.NANOSECONDS)

        /** Returns a [Duration] representing the specified [value] number of microseconds. */
        @SinceKotlin("1.5")
        public fun microseconds(value: Int): Duration = value.toDuration(DurationUnit.MICROSECONDS)

        /** Returns a [Duration] representing the specified [value] number of microseconds. */
        @SinceKotlin("1.5")
        public fun microseconds(value: Long): Duration = value.toDuration(DurationUnit.MICROSECONDS)

        /**
         * Returns a [Duration] representing the specified [value] number of microseconds.
         *
         * @throws IllegalArgumentException if the provided `Double` [value] is `NaN`.
         */
        @SinceKotlin("1.5")
        public fun microseconds(value: Double): Duration = value.toDuration(DurationUnit.MICROSECONDS)

        /** Returns a [Duration] representing the specified [value] number of milliseconds. */
        @SinceKotlin("1.5")
        public fun milliseconds(value: Int): Duration = value.toDuration(DurationUnit.MILLISECONDS)

        /** Returns a [Duration] representing the specified [value] number of milliseconds. */
        @SinceKotlin("1.5")
        public fun milliseconds(value: Long): Duration = value.toDuration(DurationUnit.MILLISECONDS)

        /**
         * Returns a [Duration] representing the specified [value] number of milliseconds.
         *
         * @throws IllegalArgumentException if the provided `Double` [value] is `NaN`.
         */
        @SinceKotlin("1.5")
        public fun milliseconds(value: Double): Duration = value.toDuration(DurationUnit.MILLISECONDS)

        /** Returns a [Duration] representing the specified [value] number of seconds. */
        @SinceKotlin("1.5")
        public fun seconds(value: Int): Duration = value.toDuration(DurationUnit.SECONDS)

        /** Returns a [Duration] representing the specified [value] number of seconds. */
        @SinceKotlin("1.5")
        public fun seconds(value: Long): Duration = value.toDuration(DurationUnit.SECONDS)

        /**
         * Returns a [Duration] representing the specified [value] number of seconds.
         *
         * @throws IllegalArgumentException if the provided `Double` [value] is `NaN`.
         */
        @SinceKotlin("1.5")
        public fun seconds(value: Double): Duration = value.toDuration(DurationUnit.SECONDS)

        /** Returns a [Duration] representing the specified [value] number of minutes. */
        @SinceKotlin("1.5")
        public fun minutes(value: Int): Duration = value.toDuration(DurationUnit.MINUTES)

        /** Returns a [Duration] representing the specified [value] number of minutes. */
        @SinceKotlin("1.5")
        public fun minutes(value: Long): Duration = value.toDuration(DurationUnit.MINUTES)

        /**
         * Returns a [Duration] representing the specified [value] number of minutes.
         *
         * @throws IllegalArgumentException if the provided `Double` [value] is `NaN`.
         */
        @SinceKotlin("1.5")
        public fun minutes(value: Double): Duration = value.toDuration(DurationUnit.MINUTES)

        /** Returns a [Duration] representing the specified [value] number of hours. */
        @SinceKotlin("1.5")
        public fun hours(value: Int): Duration = value.toDuration(DurationUnit.HOURS)

        /** Returns a [Duration] representing the specified [value] number of hours. */
        @SinceKotlin("1.5")
        public fun hours(value: Long): Duration = value.toDuration(DurationUnit.HOURS)

        /**
         * Returns a [Duration] representing the specified [value] number of hours.
         *
         * @throws IllegalArgumentException if the provided `Double` [value] is `NaN`.
         */
        @SinceKotlin("1.5")
        public fun hours(value: Double): Duration = value.toDuration(DurationUnit.HOURS)

        /** Returns a [Duration] representing the specified [value] number of days. */
        @SinceKotlin("1.5")
        public fun days(value: Int): Duration = value.toDuration(DurationUnit.DAYS)

        /** Returns a [Duration] representing the specified [value] number of days. */
        @SinceKotlin("1.5")
        public fun days(value: Long): Duration = value.toDuration(DurationUnit.DAYS)

        /**
         * Returns a [Duration] representing the specified [value] number of days.
         *
         * @throws IllegalArgumentException if the provided `Double` [value] is `NaN`.
         */
        @SinceKotlin("1.5")
        public fun days(value: Double): Duration = value.toDuration(DurationUnit.DAYS)

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
                    throw IllegalArgumentException("Summing infinite durations of different signs yields an undefined result.")
            }
            other.isInfinite() -> return other
        }

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
     *   If the value doesn't fit in [Int] range, i.e. it's greater than [Int.MAX_VALUE] or less than [Int.MIN_VALUE],
     *   it is coerced into that range.
     */
    public inline fun <T> toComponents(action: (days: Int, hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
        return action(toInt(DurationUnit.DAYS), hoursComponent, minutesComponent, secondsComponent, nanosecondsComponent)
    }

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
    public inline fun <T> toComponents(action: (hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
        return action(toInt(DurationUnit.HOURS), minutesComponent, secondsComponent, nanosecondsComponent)
    }

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
    public inline fun <T> toComponents(action: (minutes: Int, seconds: Int, nanoseconds: Int) -> T): T {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
        return action(toInt(DurationUnit.MINUTES), secondsComponent, nanosecondsComponent)
    }

    /**
     * Splits this duration into seconds, and nanoseconds and executes the given [action] with these components.
     * The result of [action] is returned as the result of this function.
     *
     * - `nanoseconds` represents the whole number of nanoseconds in this duration, and its absolute value is less than 1_000_000_000;
     * - `seconds` represents the whole number of seconds in this duration.
     *   If the value doesn't fit in [Long] range, i.e. it's greater than [Long.MAX_VALUE] or less than [Long.MIN_VALUE],
     *   it is coerced into that range.
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
     * An infinite duration value is converted either to [Double.POSITIVE_INFINITY] or [Double.NEGATIVE_INFINITY] depending on its sign.
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
     * If the result doesn't fit in the range of [Long] type, it is coerced into that range:
     * - [Long.MIN_VALUE] is returned if it's less than `Long.MIN_VALUE`,
     * - [Long.MAX_VALUE] is returned if it's greater than `Long.MAX_VALUE`.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
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
     * If the result doesn't fit in the range of [Int] type, it is coerced into that range:
     * - [Int.MIN_VALUE] is returned if it's less than `Int.MIN_VALUE`,
     * - [Int.MAX_VALUE] is returned if it's greater than `Int.MAX_VALUE`.
     *
     * An infinite duration value is converted either to [Int.MAX_VALUE] or [Int.MIN_VALUE] depending on its sign.
     */
    public fun toInt(unit: DurationUnit): Int =
        toLong(unit).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    /** The value of this duration expressed as a [Double] number of days. */
    @Deprecated("Use inWholeDays property instead or convert toDouble(DAYS) if a double value is required.", ReplaceWith("toDouble(DurationUnit.DAYS)"))
    public val inDays: Double get() = toDouble(DurationUnit.DAYS)

    /** The value of this duration expressed as a [Double] number of hours. */
    @Deprecated("Use inWholeHours property instead or convert toDouble(HOURS) if a double value is required.", ReplaceWith("toDouble(DurationUnit.HOURS)"))
    public val inHours: Double get() = toDouble(DurationUnit.HOURS)

    /** The value of this duration expressed as a [Double] number of minutes. */
    @Deprecated("Use inWholeMinutes property instead or convert toDouble(MINUTES) if a double value is required.", ReplaceWith("toDouble(DurationUnit.MINUTES)"))
    public val inMinutes: Double get() = toDouble(DurationUnit.MINUTES)

    /** The value of this duration expressed as a [Double] number of seconds. */
    @Deprecated("Use inWholeSeconds property instead or convert toDouble(SECONDS) if a double value is required.", ReplaceWith("toDouble(DurationUnit.SECONDS)"))
    public val inSeconds: Double get() = toDouble(DurationUnit.SECONDS)

    /** The value of this duration expressed as a [Double] number of milliseconds. */
    @Deprecated("Use inWholeMilliseconds property instead or convert toDouble(MILLISECONDS) if a double value is required.", ReplaceWith("toDouble(DurationUnit.MILLISECONDS)"))
    public val inMilliseconds: Double get() = toDouble(DurationUnit.MILLISECONDS)

    /** The value of this duration expressed as a [Double] number of microseconds. */
    @Deprecated("Use inWholeMicroseconds property instead or convert toDouble(MICROSECONDS) if a double value is required.", ReplaceWith("toDouble(DurationUnit.MICROSECONDS)"))
    public val inMicroseconds: Double get() = toDouble(DurationUnit.MICROSECONDS)

    /** The value of this duration expressed as a [Double] number of nanoseconds. */
    @Deprecated("Use inWholeNanoseconds property instead or convert toDouble(NANOSECONDS) if a double value is required.", ReplaceWith("toDouble(DurationUnit.NANOSECONDS)"))
    public val inNanoseconds: Double get() = toDouble(DurationUnit.NANOSECONDS)


    /**
     * The value of this duration expressed as a [Long] number of days.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     */
    @SinceKotlin("1.5")
    public val inWholeDays: Long
        get() = toLong(DurationUnit.DAYS)

    /**
     * The value of this duration expressed as a [Long] number of hours.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     */
    @SinceKotlin("1.5")
    public val inWholeHours: Long
        get() = toLong(DurationUnit.HOURS)

    /**
     * The value of this duration expressed as a [Long] number of minutes.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     */
    @SinceKotlin("1.5")
    public val inWholeMinutes: Long
        get() = toLong(DurationUnit.MINUTES)

    /**
     * The value of this duration expressed as a [Long] number of seconds.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     */
    @SinceKotlin("1.5")
    public val inWholeSeconds: Long
        get() = toLong(DurationUnit.SECONDS)

    /**
     * The value of this duration expressed as a [Long] number of milliseconds.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     */
    @SinceKotlin("1.5")
    public val inWholeMilliseconds: Long
        get() {
            return if (isInMillis() && isFinite()) value else toLong(DurationUnit.MILLISECONDS)
        }

    /**
     * The value of this duration expressed as a [Long] number of microseconds.
     *
     * If the result doesn't fit in the range of [Long] type, it is coerced into that range:
     * - [Long.MIN_VALUE] is returned if it's less than `Long.MIN_VALUE`,
     * - [Long.MAX_VALUE] is returned if it's greater than `Long.MAX_VALUE`.
     *
     * An infinite duration value is converted either to [Long.MAX_VALUE] or [Long.MIN_VALUE] depending on its sign.
     */
    @SinceKotlin("1.5")
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
     */
    @SinceKotlin("1.5")
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

    // shortcuts

    /**
     * Returns the value of this duration expressed as a [Long] number of nanoseconds.
     *
     * If the value doesn't fit in the range of [Long] type, it is coerced into that range, see the conversion [Double.toLong] for details.
     *
     * The range of durations that can be expressed as a `Long` number of nanoseconds is approximately ±292 years.
     */
    @Deprecated("Use inWholeNanoseconds property instead.", ReplaceWith("this.inWholeNanoseconds"))
    public fun toLongNanoseconds(): Long = inWholeNanoseconds

    /**
     * Returns the value of this duration expressed as a [Long] number of milliseconds.
     *
     * The value is coerced to the range of [Long] type, if it doesn't fit in that range, see the conversion [Double.toLong] for details.
     *
     * The range of durations that can be expressed as a `Long` number of milliseconds is approximately ±292 million years.
     */
    @Deprecated("Use inWholeMilliseconds property instead.", ReplaceWith("this.inWholeMilliseconds"))
    public fun toLongMilliseconds(): Long = inWholeMilliseconds

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
    override fun toString(): String = when (rawValue) {
        0L -> "0s"
        INFINITE.rawValue -> "Infinity"
        NEG_INFINITE.rawValue -> "-Infinity"
        else -> {
            val absNs = absoluteValue.toDouble(DurationUnit.NANOSECONDS)
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
        val number = toDouble(unit)
        if (number.isInfinite()) return number.toString()
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
public fun Int.toDuration(unit: DurationUnit): Duration {
    return if (unit <= DurationUnit.SECONDS) {
        durationOfNanos(convertDurationUnitOverflow(this.toLong(), unit, DurationUnit.NANOSECONDS))
    } else
        toLong().toDuration(unit)
}

/** Returns a [Duration] equal to this [Long] number of the specified [unit]. */
@SinceKotlin("1.3")
@ExperimentalTime
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
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.3")
@ExperimentalTime
public fun Double.toDuration(unit: DurationUnit): Duration {
    val valueInNs = convertDurationUnit(this, unit, DurationUnit.NANOSECONDS)
    require(!valueInNs.isNaN()) { "Duration value cannot be NaN." }
    val nanos = valueInNs.toLong()
    return if (nanos in -MAX_NANOS..MAX_NANOS) {
        durationOfNanos(nanos)
    } else {
        val millis = convertDurationUnit(this, unit, DurationUnit.MILLISECONDS).toLong()
        durationOfMillisNormalized(millis)
    }
}

// constructing from number of units
// extension properties

/** Returns a [Duration] equal to this [Int] number of nanoseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.nanoseconds() function instead.", ReplaceWith("Duration.nanoseconds(this)", "kotlin.time.Duration"))
public val Int.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

/** Returns a [Duration] equal to this [Long] number of nanoseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.nanoseconds() function instead.", ReplaceWith("Duration.nanoseconds(this)", "kotlin.time.Duration"))
public val Long.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

/**
 * Returns a [Duration] equal to this [Double] number of nanoseconds.
 *
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.nanoseconds() function instead.", ReplaceWith("Duration.nanoseconds(this)", "kotlin.time.Duration"))
public val Double.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

/** Returns a [Duration] equal to this [Int] number of microseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.microseconds() function instead.", ReplaceWith("Duration.microseconds(this)", "kotlin.time.Duration"))
public val Int.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

/** Returns a [Duration] equal to this [Long] number of microseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.microseconds() function instead.", ReplaceWith("Duration.microseconds(this)", "kotlin.time.Duration"))
public val Long.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

/**
 * Returns a [Duration] equal to this [Double] number of microseconds.
 *
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.microseconds() function instead.", ReplaceWith("Duration.microseconds(this)", "kotlin.time.Duration"))
public val Double.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

/** Returns a [Duration] equal to this [Int] number of milliseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.milliseconds() function instead.", ReplaceWith("Duration.milliseconds(this)", "kotlin.time.Duration"))
public val Int.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

/** Returns a [Duration] equal to this [Long] number of milliseconds. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.milliseconds() function instead.", ReplaceWith("Duration.milliseconds(this)", "kotlin.time.Duration"))
public val Long.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

/**
 * Returns a [Duration] equal to this [Double] number of milliseconds.
 *
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.milliseconds() function instead.", ReplaceWith("Duration.milliseconds(this)", "kotlin.time.Duration"))
public val Double.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

/** Returns a [Duration] equal to this [Int] number of seconds. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.seconds() function instead.", ReplaceWith("Duration.seconds(this)", "kotlin.time.Duration"))
public val Int.seconds get() = toDuration(DurationUnit.SECONDS)

/** Returns a [Duration] equal to this [Long] number of seconds. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.seconds() function instead.", ReplaceWith("Duration.seconds(this)", "kotlin.time.Duration"))
public val Long.seconds get() = toDuration(DurationUnit.SECONDS)

/**
 * Returns a [Duration] equal to this [Double] number of seconds.
 *
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.seconds() function instead.", ReplaceWith("Duration.seconds(this)", "kotlin.time.Duration"))
public val Double.seconds get() = toDuration(DurationUnit.SECONDS)

/** Returns a [Duration] equal to this [Int] number of minutes. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.minutes() function instead.", ReplaceWith("Duration.minutes(this)", "kotlin.time.Duration"))
public val Int.minutes get() = toDuration(DurationUnit.MINUTES)

/** Returns a [Duration] equal to this [Long] number of minutes. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.minutes() function instead.", ReplaceWith("Duration.minutes(this)", "kotlin.time.Duration"))
public val Long.minutes get() = toDuration(DurationUnit.MINUTES)

/**
 * Returns a [Duration] equal to this [Double] number of minutes.
 *
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.minutes() function instead.", ReplaceWith("Duration.minutes(this)", "kotlin.time.Duration"))
public val Double.minutes get() = toDuration(DurationUnit.MINUTES)

/** Returns a [Duration] equal to this [Int] number of hours. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.hours() function instead.", ReplaceWith("Duration.hours(this)", "kotlin.time.Duration"))
public val Int.hours get() = toDuration(DurationUnit.HOURS)

/** Returns a [Duration] equal to this [Long] number of hours. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.hours() function instead.", ReplaceWith("Duration.hours(this)", "kotlin.time.Duration"))
public val Long.hours get() = toDuration(DurationUnit.HOURS)

/**
 * Returns a [Duration] equal to this [Double] number of hours.
 *
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.hours() function instead.", ReplaceWith("Duration.hours(this)", "kotlin.time.Duration"))
public val Double.hours get() = toDuration(DurationUnit.HOURS)

/** Returns a [Duration] equal to this [Int] number of days. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.days() function instead.", ReplaceWith("Duration.days(this)", "kotlin.time.Duration"))
public val Int.days get() = toDuration(DurationUnit.DAYS)

/** Returns a [Duration] equal to this [Long] number of days. */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.days() function instead.", ReplaceWith("Duration.days(this)", "kotlin.time.Duration"))
public val Long.days get() = toDuration(DurationUnit.DAYS)

/**
 * Returns a [Duration] equal to this [Double] number of days.
 *
 * @throws IllegalArgumentException if this `Double` value is `NaN`.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@Deprecated("Use Duration.days() function instead.", ReplaceWith("Duration.days(this)", "kotlin.time.Duration"))
public val Double.days get() = toDuration(DurationUnit.DAYS)


/** Returns a duration whose value is the specified [duration] value multiplied by this number. */
@SinceKotlin("1.3")
@ExperimentalTime
@kotlin.internal.InlineOnly
public inline operator fun Int.times(duration: Duration): Duration = duration * this

/**
 * Returns a duration whose value is the specified [duration] value multiplied by this number.
 *
 * @throws IllegalArgumentException if the operation results in a `NaN` value.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@kotlin.internal.InlineOnly
public inline operator fun Double.times(duration: Duration): Duration = duration * this


// The ranges are chosen so that they are:
// - symmetric relative to zero: this greatly simplifies operations with sign, e.g. unaryMinus and minus.
// - non-overlapping, but adjacent: the first value that doesn't fit in nanos range, can be exactly represented in millis.

internal const val NANOS_IN_MILLIS = 1_000_000
// maximum number duration can store in nanosecond range
internal const val MAX_NANOS = Long.MAX_VALUE / 2 / NANOS_IN_MILLIS * NANOS_IN_MILLIS - 1 // ends in ..._999_999
// maximum number duration can store in millisecond range, also encodes an infinite value
internal const val MAX_MILLIS = Long.MAX_VALUE / 2
// MAX_NANOS expressed in milliseconds
private const val MAX_NANOS_IN_MILLIS = MAX_NANOS / NANOS_IN_MILLIS

private fun nanosToMillis(nanos: Long): Long = nanos / NANOS_IN_MILLIS
private fun millisToNanos(millis: Long): Long = millis * NANOS_IN_MILLIS

@ExperimentalTime private fun durationOfNanos(normalNanos: Long) = Duration(normalNanos shl 1)
@ExperimentalTime private fun durationOfMillis(normalMillis: Long) = Duration((normalMillis shl 1) + 1)
@ExperimentalTime private fun durationOf(normalValue: Long, unitDiscriminator: Int) = Duration((normalValue shl 1) + unitDiscriminator)
@ExperimentalTime private fun durationOfNanosNormalized(nanos: Long) =
    if (nanos in -MAX_NANOS..MAX_NANOS) {
        durationOfNanos(nanos)
    } else {
        durationOfMillis(nanosToMillis(nanos))
    }

@ExperimentalTime private fun durationOfMillisNormalized(millis: Long) =
    if (millis in -MAX_NANOS_IN_MILLIS..MAX_NANOS_IN_MILLIS) {
        durationOfNanos(millisToNanos(millis))
    } else {
        durationOfMillis(millis.coerceIn(-MAX_MILLIS, MAX_MILLIS))
    }


internal expect fun formatToExactDecimals(value: Double, decimals: Int): String
internal expect fun formatUpToDecimals(value: Double, decimals: Int): String
internal expect fun formatScientific(value: Double): String