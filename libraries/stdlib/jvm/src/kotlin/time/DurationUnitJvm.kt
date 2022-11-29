/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass()
@file:kotlin.jvm.JvmName("DurationUnitKt")

package kotlin.time

import java.util.concurrent.TimeUnit

@SinceKotlin("1.6")
@WasExperimental(ExperimentalTime::class)
public actual enum class DurationUnit(internal val timeUnit: TimeUnit) {
    /**
     * Time unit representing one nanosecond, which is 1/1000 of a microsecond.
     */
    NANOSECONDS(TimeUnit.NANOSECONDS),
    /**
     * Time unit representing one microsecond, which is 1/1000 of a millisecond.
     */
    MICROSECONDS(TimeUnit.MICROSECONDS),
    /**
     * Time unit representing one millisecond, which is 1/1000 of a second.
     */
    MILLISECONDS(TimeUnit.MILLISECONDS),
    /**
     * Time unit representing one second.
     */
    SECONDS(TimeUnit.SECONDS),
    /**
     * Time unit representing one minute.
     */
    MINUTES(TimeUnit.MINUTES),
    /**
     * Time unit representing one hour.
     */
    HOURS(TimeUnit.HOURS),
    /**
     * Time unit representing one day, which is always equal to 24 hours.
     */
    DAYS(TimeUnit.DAYS);
}

/**
 * Converts this [kotlin.time.DurationUnit][DurationUnit] enum value to the corresponding [java.util.concurrent.TimeUnit][java.util.concurrent.TimeUnit] value.
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalTime::class)
public fun DurationUnit.toTimeUnit(): TimeUnit = timeUnit

/**
 * Converts this [java.util.concurrent.TimeUnit][java.util.concurrent.TimeUnit] enum value to the corresponding [kotlin.time.DurationUnit][DurationUnit] value.
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalTime::class)
public fun TimeUnit.toDurationUnit(): DurationUnit = when (this) {
    TimeUnit.NANOSECONDS -> DurationUnit.NANOSECONDS
    TimeUnit.MICROSECONDS -> DurationUnit.MICROSECONDS
    TimeUnit.MILLISECONDS -> DurationUnit.MILLISECONDS
    TimeUnit.SECONDS -> DurationUnit.SECONDS
    TimeUnit.MINUTES -> DurationUnit.MINUTES
    TimeUnit.HOURS -> DurationUnit.HOURS
    TimeUnit.DAYS -> DurationUnit.DAYS
}

@SinceKotlin("1.3")
internal actual fun convertDurationUnit(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double {
    val sourceInTargets = targetUnit.timeUnit.convert(1, sourceUnit.timeUnit)
    if (sourceInTargets > 0)
        return value * sourceInTargets

    val otherInThis = sourceUnit.timeUnit.convert(1, targetUnit.timeUnit)
    return value / otherInThis
}

@SinceKotlin("1.5")
internal actual fun convertDurationUnitOverflow(value: Long, sourceUnit: DurationUnit, targetUnit: DurationUnit): Long {
    return targetUnit.timeUnit.convert(value, sourceUnit.timeUnit)
}

@SinceKotlin("1.5")
internal actual fun convertDurationUnit(value: Long, sourceUnit: DurationUnit, targetUnit: DurationUnit): Long {
    return targetUnit.timeUnit.convert(value, sourceUnit.timeUnit)
}
