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

private const val MAX_LONG_63 = Long.MAX_VALUE / 2
private const val MIN_LONG_63 = Long.MIN_VALUE / 2
private const val NANOS_IN_MILLIS = 1_000_000
private const val MAX_MILLIS_IN_NANOS = MAX_LONG_63 / NANOS_IN_MILLIS
private const val MIN_MILLIS_IN_NANOS = MIN_LONG_63 / NANOS_IN_MILLIS

private fun nanosToMillis(value: Long): Long = value / NANOS_IN_MILLIS
private fun millisToNanos(value: Long): Long = value * NANOS_IN_MILLIS
private fun storeAsMillis(value: Long): Long = (value shl 1) + 1
private fun storeAsNanos(value: Long): Long = value shl 1
private fun storeAs(value: Long, unitValue: Long): Long = (value shl 1) + (unitValue and 1)

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
 * or the properties [inHours], [inMinutes], [inSeconds], [inNanoseconds], and so on.
 */
@SinceKotlin("1.3")
@ExperimentalTime
@JvmInline
public value class Duration internal constructor(internal val rawValue: Long) : Comparable<Duration> {

    private fun isInNanos() = rawValue and 1L == 0L
    private fun isInMillis() = rawValue and 1L == 1L
    private val value: Long get() = rawValue shr 1
    private val storageUnit get() = if (isInNanos()) DurationUnit.NANOSECONDS else DurationUnit.MILLISECONDS

    init {
        // TODO: assert invariants
    }

    companion object {
        /** The duration equal to exactly 0 seconds. */
        public val ZERO: Duration = Duration(0L)

        /** The duration whose value is positive infinity. It is useful for representing timeouts that should never expire. */
        public val INFINITE: Duration = Duration(storeAsMillis(MAX_LONG_63))

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
    public operator fun unaryMinus(): Duration {
        val value = value
        return when {
            rawValue == INFINITE.rawValue -> // negate positive infinite to negative infinite
                Duration(storeAsMillis(MIN_LONG_63))
            value != MIN_LONG_63 ->
                Duration(storeAs(-value, rawValue))
            isInNanos() -> // negating MIN_VALUE in nanos overflows to millisecond units
                Duration(storeAsMillis(-nanosToMillis(value)))
            else ->        // negating negative infinity overflows to positive infinity
                INFINITE
        }
    }

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
                    throw IllegalArgumentException("Sum of infinite durations of different signs is undefined.")
            }
            other.isInfinite() -> return other
        }

        return if (this.isInNanos() == other.isInNanos()) {
            when (val newValue = this.value + other.value) { // never overflows long, but can overflow long63
                in MIN_LONG_63..MAX_LONG_63 -> {
                    if (isInMillis() && newValue in MIN_MILLIS_IN_NANOS..MAX_MILLIS_IN_NANOS)
                        Duration(storeAsNanos(millisToNanos(newValue)))
                    else
                        Duration(storeAs(newValue, this.rawValue))
                }
                else -> {
                    val millisValue = if (isInNanos()) nanosToMillis(newValue) else newValue.coerceIn(MIN_LONG_63, MAX_LONG_63)
                    Duration(storeAsMillis(millisValue))
                }
            }
        } else {
            val a: Long
            val b: Long
            if (this.isInMillis()) {
                a = this.value
                b = other.value
            } else {
                a = other.value
                b = this.value
            }

            val bMillis = nanosToMillis(b)
            val resultMillis = a + bMillis
            return if (resultMillis in MIN_MILLIS_IN_NANOS..MAX_MILLIS_IN_NANOS) {
                val bRemainder = b - millisToNanos(bMillis)
                Duration(storeAsNanos(millisToNanos(resultMillis) + bRemainder)) // can it overflow nanos?
            } else {
                Duration(storeAsMillis(resultMillis.coerceIn(MIN_LONG_63, MAX_LONG_63)))
            }
        }
    }

    /**
     * Returns a duration whose value is the difference between this and [other] duration values.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * e.g. when subtracting infinite durations of the same sign.
     */
    // TODO: negation may overflow
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
                scale == 0 -> throw IllegalArgumentException("Multiplying infinite duration by zero is undefined.")
                scale > 0 -> this
                else -> -this
            }
        }
        if (scale == 0) return ZERO

        val result = value * scale
        if (value >= Int.MIN_VALUE && value <= Int.MAX_VALUE) {
            return Duration(storeAs(result, rawValue))
        }
        if (result / scale != value && isInNanos()) { // Long overflow
            val rawMillis = nanosToMillis(value)
            val resultMillis = rawMillis * scale
            if (resultMillis / scale != rawMillis) return Duration.INFINITE
            return when {
                resultMillis in MIN_LONG_63..MAX_LONG_63 -> Duration(storeAsMillis(resultMillis))
                resultMillis > 0 -> Duration.INFINITE
                else -> -Duration.INFINITE
            }
        }

        return when {
            result in MIN_LONG_63..MAX_LONG_63 -> {
                Duration(storeAs(result, rawValue))
            }
            isInNanos() -> {
                Duration(storeAsMillis(nanosToMillis(result)))
            }
            result > 0 -> Duration.INFINITE
            else -> -Duration.INFINITE
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
                isPositive() -> Duration.INFINITE
                isNegative() -> -Duration.INFINITE
                else -> throw IllegalArgumentException("Dividing zero duration by zero is undefined")
            }
        }
        if (isInNanos()) {
            return Duration(storeAsNanos(value / scale))
        } else {
            if (isInfinite())
                return this * scale.sign

            val result = value / scale

            if (result in MIN_MILLIS_IN_NANOS..MAX_MILLIS_IN_NANOS) {
                // TODO: more precise
                return Duration(storeAsNanos(millisToNanos(result)))
            }
            return Duration(storeAsMillis(result))
        }
    }

    /**
     * Returns a duration whose value is this duration value divided by the given [scale] number.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * e.g. when dividing an infinite duration by infinity or zero duration by zero.
     */
    public operator fun div(scale: Double): Duration {
        if (scale != 0.0) {
            val intScale = scale.roundToInt()
            if (intScale.toDouble() == scale) {
                return div(intScale)
            }
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
    public fun isInfinite(): Boolean = isInMillis() && value.let { it == MAX_LONG_63 || it == MIN_LONG_63 }

    /** Returns true, if the duration value is finite. */
    public fun isFinite(): Boolean = !isInfinite()

    /** Returns the absolute value of this value. The returned value is always non-negative. */
    public val absoluteValue: Duration get() = if (isNegative()) -this else this

    override fun compareTo(other: Duration): Int {
        val compareBits = this.rawValue xor other.rawValue
        if (compareBits < 0 || compareBits and 1L == 0L) // different signs or same sign, same scale
            return this.rawValue.compareTo(other.rawValue)
        // same sign, different scales
        val r = (this.rawValue and 1L).toInt() - (other.rawValue and 1L).toInt()
        return if (this.rawValue < 0) -r else r
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
        return action(inDays.toInt(), hoursComponent, minutesComponent, secondsComponent, nanosecondsComponent)
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
        return action(inHours.toInt(), minutesComponent, secondsComponent, nanosecondsComponent)
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
        return action(inMinutes.toInt(), secondsComponent, nanosecondsComponent)
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
        return action(inSeconds.toLong(), nanosecondsComponent)
    }

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
    public fun toDouble(unit: DurationUnit): Double {
        return if (isInfinite()) {
            if (isNegative()) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        } else {
            // TODO: whether it's ok to convert to Double before scaling
            convertDurationUnit(value.toDouble(), storageUnit, unit)
        }
    }

    /**
     * Returns the value of this duration expressed as a [Long] number of the specified [unit].
     *
     * If the value doesn't fit in the range of [Long] type, it is coerced into that range, see the conversion [Double.toLong] for details.
     */
    public fun toLong(unit: DurationUnit): Long {
        return if (isInfinite()) {
            if (isNegative()) Long.MIN_VALUE else Long.MAX_VALUE
        } else {
            convertDurationUnit(value, storageUnit, unit)
        }
    }

    /**
     * Returns the value of this duration expressed as an [Int] number of the specified [unit].
     *
     * If the value doesn't fit in the range of [Int] type, it is coerced into that range, see the conversion [Double.toInt] for details.
     */
    public fun toInt(unit: DurationUnit): Int =
        toLong(unit).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    // TODO: inWholeUnits conversions, deprecate inUnits

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
    // TODO: Deprecate in favor inWholeNanoseconds
    public fun toLongNanoseconds(): Long {
        val value = value
        return when {
            isInNanos() -> value
            value > Long.MAX_VALUE / NANOS_IN_MILLIS -> Long.MAX_VALUE
            value < Long.MIN_VALUE / NANOS_IN_MILLIS -> Long.MIN_VALUE
            else -> millisToNanos(value)
        }
    }

    /**
     * Returns the value of this duration expressed as a [Long] number of milliseconds.
     *
     * The value is coerced to the range of [Long] type, if it doesn't fit in that range, see the conversion [Double.toLong] for details.
     *
     * The range of durations that can be expressed as a `Long` number of milliseconds is approximately ±292 million years.
     */
    // TODO: Deprecate in favor inWholeMilliseconds
    public fun toLongMilliseconds(): Long {
        return if (isInMillis() && isFinite()) value else toLong(DurationUnit.MILLISECONDS)
    }

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
        isInfinite() -> if (isNegative()) "-Infinity" else "Infinity"
        rawValue == 0L -> "0s"
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
    @OptIn(ExperimentalStdlibApi::class)
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
        Duration(storeAsNanos(convertDurationUnitOverflow(this.toLong(), unit, DurationUnit.NANOSECONDS)))
    } else
        toLong().toDuration(unit)
}

/** Returns a [Duration] equal to this [Long] number of the specified [unit]. */
@SinceKotlin("1.3")
@ExperimentalTime
public fun Long.toDuration(unit: DurationUnit): Duration {
    val maxInUnit = convertDurationUnitOverflow(MAX_LONG_63, DurationUnit.NANOSECONDS, unit)
    if (this in -maxInUnit..maxInUnit) {
        return Duration(storeAsNanos(convertDurationUnitOverflow(this, unit, DurationUnit.NANOSECONDS)))
    } else {
        val max2InUnit = convertDurationUnitOverflow(MAX_LONG_63, DurationUnit.MILLISECONDS, unit)
        if (unit < DurationUnit.MILLISECONDS || this in -max2InUnit..max2InUnit) {
            return Duration(storeAsMillis(convertDurationUnitOverflow(this, unit, DurationUnit.MILLISECONDS)))
        }
        return if (this < 0) -Duration.INFINITE else Duration.INFINITE
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
    return if (valueInNs > MAX_LONG_63 || valueInNs < MIN_LONG_63) {
        Duration(storeAsMillis((valueInNs / NANOS_IN_MILLIS).toLong().coerceIn(MIN_LONG_63, MAX_LONG_63)))
    } else {
        Duration(storeAsNanos(valueInNs.toLong()))
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

internal expect fun formatToExactDecimals(value: Double, decimals: Int): String
internal expect fun formatUpToDecimals(value: Double, decimals: Int): String
internal expect fun formatScientific(value: Double): String