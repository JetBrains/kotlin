/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time


/**
 * The list of possible time measurement units, in which a duration can be expressed.
 *
 * The smallest time unit is [NANOSECONDS] and the largest is [DAYS], which corresponds to exactly 24 [HOURS].
 */
@SinceKotlin("1.3")
@ExperimentalTime
public actual enum class DurationUnit {
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
@ExperimentalTime
internal actual fun convertDurationUnit(value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit): Double  = TODO("Wasm stdlib: convertDurationUnit")
