/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlin.math.abs

@UseExperimental(ExperimentalTime::class)
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
@SinceKotlin("1.3")
@ExperimentalTime
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public inline class Duration internal constructor(internal val value: Double) : Comparable<Duration> {
// TODO: backend fails on init block, wait for KT-28055

//    init {
//        require(_value.isNaN().not())
//    }

    companion object {
        public val ZERO: Duration = Duration(0.0)
        public val INFINITE: Duration = Duration(Double.POSITIVE_INFINITY)

        /** Converts the given time duration [value] expressed in the specified [sourceUnit] into the specified [targetUnit]. */
        public fun convert(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double =
            convertDurationUnit(value, sourceUnit, targetUnit)
    }

    // arithmetic operators

    public operator fun unaryMinus(): Duration = Duration(-value)
    public operator fun plus(other: Duration): Duration = Duration(value + other.value)
    public operator fun minus(other: Duration): Duration = Duration(value - other.value)

    // should we declare symmetric extension operators?

    public operator fun times(scale: Int): Duration = Duration(value * scale)
    public operator fun times(scale: Double): Duration = Duration(value * scale)

    public operator fun div(scale: Int): Duration = Duration(value / scale)
    public operator fun div(scale: Double): Duration = Duration(value / scale)

    public operator fun div(other: Duration): Double = this.value / other.value

    public fun isNegative(): Boolean = value < 0
    public fun isInfinite(): Boolean = value.isInfinite()
    public fun isFinite(): Boolean = value.isFinite()

    public val absoluteValue: Duration get() = if (isNegative()) -this else this


    override fun compareTo(other: Duration): Int = this.value.compareTo(other.value)


    // splitting to components

    public inline fun <T> toComponents(action: (days: Int, hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T =
        action(inDays.toInt(), hoursComponent, minutesComponent, secondsComponent, nanosecondsComponent)

    public inline fun <T> toComponents(action: (hours: Int, minutes: Int, seconds: Int, nanoseconds: Int) -> T): T =
        action(inHours.toInt(), minutesComponent, secondsComponent, nanosecondsComponent)

    public inline fun <T> toComponents(action: (minutes: Int, seconds: Int, nanoseconds: Int) -> T): T =
        action(inMinutes.toInt(), secondsComponent, nanosecondsComponent)

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

    public fun toDouble(unit: DurationUnit): Double = convertDurationUnit(value, storageUnit, unit)
    public fun toLong(unit: DurationUnit): Long = toDouble(unit).toLong()
    public fun toInt(unit: DurationUnit): Int = toDouble(unit).toInt()

    // option 1: in- properties

    public val inDays: Double get() = toDouble(DurationUnit.DAYS)
    public val inHours: Double get() = toDouble(DurationUnit.HOURS)
    public val inMinutes: Double get() = toDouble(DurationUnit.MINUTES)
    public val inSeconds: Double get() = toDouble(DurationUnit.SECONDS)
    public val inMilliseconds: Double get() = toDouble(DurationUnit.MILLISECONDS)
    public val inMicroseconds: Double get() = toDouble(DurationUnit.MICROSECONDS)
    public val inNanoseconds: Double get() = toDouble(DurationUnit.NANOSECONDS)

    // shortcuts

    public fun toLongNanoseconds(): Long = toLong(DurationUnit.NANOSECONDS)
    public fun toLongMilliseconds(): Long = toLong(DurationUnit.MILLISECONDS)

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

    public fun toString(unit: DurationUnit, decimals: Int = 0): String {
        require(decimals >= 0) { "decimals must be not negative, but was $decimals" }
        if (isInfinite()) return value.toString()
        return formatToExactDecimals(toDouble(unit), decimals) + unit.shortName()
    }


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

// constructing from number of units
// extension functions

@SinceKotlin("1.3")
@ExperimentalTime
public fun Int.toDuration(unit: DurationUnit): Duration = toDouble().toDuration(unit)

@SinceKotlin("1.3")
@ExperimentalTime
public fun Long.toDuration(unit: DurationUnit): Duration = toDouble().toDuration(unit)

@SinceKotlin("1.3")
@ExperimentalTime
public fun Double.toDuration(unit: DurationUnit): Duration = Duration(convertDurationUnit(this, unit, storageUnit))

// constructing from number of units
// extension properties

@SinceKotlin("1.3")
@ExperimentalTime
public val Int.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Long.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Double.nanoseconds get() = toDuration(DurationUnit.NANOSECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Int.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Long.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Double.microseconds get() = toDuration(DurationUnit.MICROSECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Int.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Long.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Double.milliseconds get() = toDuration(DurationUnit.MILLISECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Int.seconds get() = toDuration(DurationUnit.SECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Long.seconds get() = toDuration(DurationUnit.SECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Double.seconds get() = toDuration(DurationUnit.SECONDS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Int.minutes get() = toDuration(DurationUnit.MINUTES)

@SinceKotlin("1.3")
@ExperimentalTime
public val Long.minutes get() = toDuration(DurationUnit.MINUTES)

@SinceKotlin("1.3")
@ExperimentalTime
public val Double.minutes get() = toDuration(DurationUnit.MINUTES)

@SinceKotlin("1.3")
@ExperimentalTime
public val Int.hours get() = toDuration(DurationUnit.HOURS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Long.hours get() = toDuration(DurationUnit.HOURS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Double.hours get() = toDuration(DurationUnit.HOURS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Int.days get() = toDuration(DurationUnit.DAYS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Long.days get() = toDuration(DurationUnit.DAYS)

@SinceKotlin("1.3")
@ExperimentalTime
public val Double.days get() = toDuration(DurationUnit.DAYS)


internal expect fun formatToExactDecimals(value: Double, decimals: Int): String
internal expect fun formatUpToDecimals(value: Double, decimals: Int): String
internal expect fun formatScientific(value: Double): String