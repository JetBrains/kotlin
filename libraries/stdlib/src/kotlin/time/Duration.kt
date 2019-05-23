/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.math.abs

private val storageUnit = DurationUnit.NANOSECONDS

/**
 * Represents the amount of time one instant of time is away from another instant.
 *
 * A negative duration is possible in a situation when the second instant is earlier than the first one.
 * An infinite duration value [Duration.INFINITE] can be used to represent infinite timeouts.
 *
 * To construct a duration use either the extension function [toDuration],
 * or the extension properties [hours], [minutes], [seconds] and so on,
 * available on [Int], [Long] and [Double] numeric types.
 *
 * To get the value of this duration expressed in a particular [duration units][DurationUnit]
 * use the functions [toInt], [toLong] and [toDouble]
 * or the properties [inHours], [inMinutes], [inSeconds], [inNanoseconds] and so on.
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public inline class Duration internal constructor(internal val _value: Double) : Comparable<Duration> {
// TODO: backend fails on init block, wait for KT-28055

//    init {
//        require(_value.isNaN().not())
//    }

    companion object {
        val ZERO: Duration = Duration(0.0)
        val INFINITE: Duration = Duration(Double.POSITIVE_INFINITY)
    }

    // arithmetic operators

    operator fun unaryMinus(): Duration = Duration(-_value)
    operator fun plus(other: Duration): Duration = Duration(_value + other._value)
    operator fun minus(other: Duration): Duration = Duration(_value - other._value)

    // should we declare symmetric extension operators?

    operator fun times(scale: Int): Duration = Duration(_value * scale)
    operator fun times(scale: Double): Duration = Duration(_value * scale)

    operator fun div(scale: Int): Duration = Duration(_value / scale)
    operator fun div(scale: Double): Duration = Duration(_value / scale)

    operator fun div(other: Duration): Double = this._value / other._value

    fun isNegative(): Boolean = _value < 0
    fun isInfinite(): Boolean = _value.isInfinite()
    fun isFinite(): Boolean = _value.isFinite()

    fun absoluteValue(): Duration = if (isNegative()) -this else this


    override fun compareTo(other: Duration): Int = this._value.compareTo(other._value)


    // splitting to components

    // problem: withComponents can be confused with 'wither' function
    // perhaps better name would be 'letComponents'

    inline fun <T> withComponents(action: (hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T =
        action(inHours.toInt(), minutesComponent, secondsComponent, nanosecondsComponent)

    inline fun <T> withComponents(action: (days: Int, hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T =
        action(inDays.toInt(), hoursComponent, minutesComponent, secondsComponent, nanosecondsComponent)


    @PublishedApi
    internal val hoursComponent: Int get() = (inHours % 24).toInt()
    @PublishedApi
    internal val minutesComponent: Int get() = (inMinutes % 60).toInt()
    @PublishedApi
    internal val secondsComponent: Int get() = (inSeconds % 60).toInt()
    @PublishedApi
    internal val nanosecondsComponent: Int get() = (inNanoseconds % 1e9).toInt()


    // conversion to units

    fun toDouble(unit: DurationUnit): Double = convertDurationUnit(_value, storageUnit, unit)
    fun toLong(unit: DurationUnit): Long = toDouble(unit).toLong()
    fun toInt(unit: DurationUnit): Int = toDouble(unit).toInt()

    // option 1: in- properties

    val inDays: Double get() = toDouble(DurationUnit.DAYS)
    val inHours: Double get() = toDouble(DurationUnit.HOURS)
    val inMinutes: Double get() = toDouble(DurationUnit.MINUTES)
    val inSeconds: Double get() = toDouble(DurationUnit.SECONDS)
    val inMilliseconds: Double get() = toDouble(DurationUnit.MILLISECONDS)
    val inMicroseconds: Double get() = toDouble(DurationUnit.MICROSECONDS)
    val inNanoseconds: Double get() = toDouble(DurationUnit.NANOSECONDS)

    // shortcuts

    fun toLongNanoseconds(): Long = toLong(DurationUnit.NANOSECONDS)
    fun toLongMilliseconds(): Long = toLong(DurationUnit.MILLISECONDS)

    override fun toString(): String = buildString {
        if (isInfinite()) {
            append(_value)
        } else {
            val absNs = absoluteValue().inNanoseconds
            var scientific = false
            var maxDecimals = 0
            val unit = when {
                absNs == 0.0 -> return "0s"
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
            return when {
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

    fun toString(unit: DurationUnit, decimals: Int = 0): String {
        require(decimals >= 0) { "decimals must be not negative, but was $decimals" }
        if (isInfinite()) return _value.toString()
        return formatToExactDecimals(toDouble(unit), decimals) + unit.shortName()
    }


    fun toIsoString(): String = buildString {
        if (isNegative()) append('-')
        append('P')
        absoluteValue().withComponents { days, hours, minutes, seconds, nanoseconds ->
            if (days != 0)
                append(days).append('D')


            if (days == 0 || seconds != 0 || nanoseconds != 0 || minutes != 0 || hours != 0) {
                append('T')
                val hasHours = hours != 0 || days != 0
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
                            nanoseconds % 1_000_000 == 0 -> append(nss, 0, 3)
                            nanoseconds % 1_000 == 0 -> append(nss, 0, 6)
                            else -> append(nss)
                        }
                    }
                    append('S')
                }
            }
        }
    }


}

// constructing from number of units
// extension functions

fun Int.toDuration(unit: DurationUnit): Duration = toDouble().toDuration(unit)
fun Long.toDuration(unit: DurationUnit): Duration = toDouble().toDuration(unit)
fun Double.toDuration(unit: DurationUnit): Duration = Duration(convertDurationUnit(this, unit, storageUnit))

// constructing from number of units
// extension properties

val Int.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val Long.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)
val Double.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

val Int.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val Long.microseconds get() = toDuration(DurationUnit.MICROSECONDS)
val Double.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

val Int.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val Long.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)
val Double.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

val Int.seconds get() = toDuration(DurationUnit.SECONDS)
val Long.seconds get() = toDuration(DurationUnit.SECONDS)
val Double.seconds get() = toDuration(DurationUnit.SECONDS)

val Int.minutes get() = toDuration(DurationUnit.MINUTES)
val Long.minutes get() = toDuration(DurationUnit.MINUTES)
val Double.minutes get() = toDuration(DurationUnit.MINUTES)

val Int.hours get() = toDuration(DurationUnit.HOURS)
val Long.hours get() = toDuration(DurationUnit.HOURS)
val Double.hours get() = toDuration(DurationUnit.HOURS)

val Int.days get() = toDuration(DurationUnit.DAYS)
val Long.days get() = toDuration(DurationUnit.DAYS)
val Double.days get() = toDuration(DurationUnit.DAYS)



internal expect fun formatToExactDecimals(value: Double, decimals: Int): String
internal expect fun formatUpToDecimals(value: Double, decimals: Int): String
internal expect fun formatScientific(value: Double): String