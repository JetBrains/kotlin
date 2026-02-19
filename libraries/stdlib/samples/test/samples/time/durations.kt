/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import samples.*
import kotlin.test.*

import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class Durations {

    @Sample
    fun toIsoString() {
        assertPrints(25.nanoseconds.toIsoString(), "PT0.000000025S")
        assertPrints(120.3.milliseconds.toIsoString(), "PT0.120300S")
        assertPrints(30.5.seconds.toIsoString(), "PT30.500S")
        assertPrints(30.5.minutes.toIsoString(), "PT30M30S")
        assertPrints(86420.seconds.toIsoString(), "PT24H0M20S")
        assertPrints(2.days.toIsoString(), "PT48H")
        assertPrints(Duration.ZERO.toIsoString(), "PT0S")
        assertPrints(Duration.INFINITE.toIsoString(), "PT9999999999999H")
    }

    @Sample
    fun toStringDefault() {
        assertPrints(45.days, "45d")
        assertPrints(1.5.days, "1d 12h")
        assertPrints(1230.minutes, "20h 30m")
        assertPrints(920.minutes, "15h 20m")
        assertPrints(1.546.seconds, "1.546s")
        assertPrints(25.12.milliseconds, "25.12ms")
    }

    @Sample
    fun toStringDecimals() {
        assertPrints(1230.minutes.toString(DurationUnit.DAYS, 2), "0.85d")
        assertPrints(1230.minutes.toString(DurationUnit.HOURS, 2), "20.50h")
        assertPrints(1230.minutes.toString(DurationUnit.MINUTES), "1230m")
        assertPrints(1230.minutes.toString(DurationUnit.SECONDS), "73800s")
    }

    @Sample
    fun parse() {
        val isoFormatString = "PT1H30M"
        val defaultFormatString = "1h 30m"
        val singleUnitFormatString = "1.5h"
        val invalidFormatString = "1 hour 30 minutes"

        assertPrints(Duration.parse(isoFormatString), "1h 30m")
        assertPrints(Duration.parse(defaultFormatString), "1h 30m")
        assertPrints(Duration.parse(singleUnitFormatString), "1h 30m")
        assertFails { Duration.parse(invalidFormatString) }
        assertPrints(Duration.parseOrNull(invalidFormatString), "null")
    }

    @Sample
    fun parseIsoString() {
        val isoFormatString = "PT1H30M"
        val defaultFormatString = "1h 30m"

        assertPrints(Duration.parseIsoString(isoFormatString), "1h 30m")
        assertFails { Duration.parseIsoString(defaultFormatString) }
        assertPrints(Duration.parseIsoStringOrNull(defaultFormatString), "null")
    }

    @Sample
    fun fromNanoseconds() {
        assertPrints(15.nanoseconds, "15ns")

        // Large values can be accurately represented only with millisecond precision
        assertPrints(9000000000_054_775_807.nanoseconds, "104166d 16h 0m 0.054s")

        // Fractional part is rounded to the nearest integer nanosecond value
        assertPrints(999.4.nanoseconds, "999ns")
        assertPrints(999.9.nanoseconds, "1us")

        assertFailsWith<IllegalArgumentException> { Double.NaN.nanoseconds }
    }

    @Sample
    fun fromMicroseconds() {
        assertPrints(15.microseconds, "15us")

        // Large values can be accurately represented only with millisecond precision
        assertPrints(9000000000000_775_807.microseconds, "104166666d 16h 0m 0.775s")

        // Fractional part is rounded to the nearest integer nanosecond value
        assertPrints(999.0004.microseconds, "999us")
        assertPrints(999.0009.microseconds, "999.001us")

        assertFailsWith<IllegalArgumentException> { Double.NaN.microseconds }
    }


    @Sample
    fun fromMilliseconds() {
        assertPrints(1500.milliseconds, "1.5s")

        // Large values can be converted to an infinite duration
        assertPrints(9_000_000_000_000_000_807.milliseconds, "Infinity")

        // Fractional part is rounded to the nearest integer nanosecond value
        assertPrints(2.000_000_4.milliseconds, "2ms")
        assertPrints(2.000_000_9.milliseconds, "2.000001ms")
        // Or to the nearest integer millisecond value
        assertPrints(9_000_000_000_000_807.5.milliseconds, "104166666d 16h 0m 0.808s")

        assertFailsWith<IllegalArgumentException> { Double.NaN.milliseconds }
    }


    @Sample
    fun fromSeconds() {
        assertPrints(61.seconds, "1m 1s")
        assertPrints(0.5.seconds, "500ms")

        // Large values can be converted to an infinite duration
        assertPrints(9_000_000_000_000_000.seconds, "Infinity")

        // Fractional part is rounded to the nearest integer nanosecond value
        assertPrints(2.000_000_000_4.seconds, "2s")
        assertPrints(2.000_000_000_9.seconds, "2.000000001s")
        // Or to the nearest integer millisecond value
        assertPrints(9_000_000_000_000.001_5.seconds, "104166666d 16h 0m 0.002s")

        assertFailsWith<IllegalArgumentException> { Double.NaN.seconds }
    }

    @Sample
    fun fromMinutes() {
        assertPrints(61.minutes, "1h 1m")
        assertPrints(0.5.minutes, "30s")

        // Large values can be converted to an infinite duration
        assertPrints(90_000_000_000_000.minutes, "Infinity")

        assertFailsWith<IllegalArgumentException> { Double.NaN.minutes }
    }

    @Sample
    fun fromHours() {
        assertPrints(26.hours, "1d 2h")
        assertPrints(0.5.hours, "30m")

        // Large values can be converted to an infinite duration
        assertPrints(10_000_000_000_000.hours, "Infinity")

        assertFailsWith<IllegalArgumentException> { Double.NaN.hours }
    }

    @Sample
    fun fromDays() {
        assertPrints(366.days, "366d")
        assertPrints(0.5.days, "12h")

        // Large values can be converted to an infinite duration
        assertPrints(100_000_000_000.days, "Infinity")

        assertFailsWith<IllegalArgumentException> { Double.NaN.days }
    }

    @Sample
    fun inWholeNanoseconds() {
        assertPrints(3.milliseconds.inWholeNanoseconds, "3000000")

        assertTrue(1_000_000_000.days.inWholeNanoseconds == Long.MAX_VALUE)
        assertTrue((-Duration.INFINITE).inWholeNanoseconds == Long.MIN_VALUE)
    }

    @Sample
    fun inWholeMicroseconds() {
        assertPrints(25_900.nanoseconds.inWholeMicroseconds, "25")

        assertTrue(1_000_000_000.days.inWholeMicroseconds == Long.MAX_VALUE)
        assertTrue((-Duration.INFINITE).inWholeMicroseconds == Long.MIN_VALUE)
    }

    @Sample
    fun inWholeMilliseconds() {
        assertPrints(25_999_999.nanoseconds.inWholeMilliseconds, "25")

        assertTrue((-Duration.INFINITE).inWholeMilliseconds == Long.MIN_VALUE)
    }

    @Sample
    fun inWholeSeconds() {
        assertPrints(25_900.milliseconds.inWholeSeconds, "25")

        assertTrue((-Duration.INFINITE).inWholeSeconds == Long.MIN_VALUE)
    }

    @Sample
    fun inWholeMinutes() {
        assertPrints(59.seconds.inWholeMinutes, "0")
        assertPrints(120.seconds.inWholeMinutes, "2")

        assertTrue((-Duration.INFINITE).inWholeMinutes == Long.MIN_VALUE)
    }

    @Sample
    fun inWholeHours() {
        assertPrints(59.minutes.inWholeHours, "0")
        assertPrints(120.minutes.inWholeHours, "2")

        assertTrue((-Duration.INFINITE).inWholeHours == Long.MIN_VALUE)
    }


    @Sample
    fun inWholeDays() {
        assertPrints(23.5.hours.inWholeDays, "0")
        assertPrints(48.hours.inWholeDays, "2")

        assertTrue((-Duration.INFINITE).inWholeDays == Long.MIN_VALUE)
    }

    @Sample
    fun toIntUnits() {
        assertPrints(2900.milliseconds.toInt(DurationUnit.SECONDS), "2")
        assertPrints(3.hours.toInt(DurationUnit.MINUTES), "180")

        assertTrue(1.minutes.toInt(DurationUnit.NANOSECONDS) == Int.MAX_VALUE)
        assertTrue((-Duration.INFINITE).toInt(DurationUnit.DAYS) == Int.MIN_VALUE)
    }


    @Sample
    fun toLongUnits() {
        assertPrints(2900.milliseconds.toLong(DurationUnit.SECONDS), "2")
        assertPrints(3.hours.toLong(DurationUnit.MINUTES), "180")
        assertPrints(1.minutes.toLong(DurationUnit.NANOSECONDS), "60000000000")

        assertTrue((365 * 300).days.toLong(DurationUnit.NANOSECONDS) == Long.MAX_VALUE)
        assertTrue((-Duration.INFINITE).toLong(DurationUnit.DAYS) == Long.MIN_VALUE)
    }

    @Sample
    fun toDoubleUnits() {
        assertPrints(2900.milliseconds.toDouble(DurationUnit.SECONDS), "2.9")
        assertPrints(1.seconds.toDouble(DurationUnit.MINUTES), "0.016666666666666666")

        assertPrints(Duration.INFINITE.toDouble(DurationUnit.SECONDS), "Infinity")
    }

    @Sample
    fun toDurationInt() {
        assertPrints(100.toDuration(DurationUnit.MILLISECONDS), "100ms")
        assertPrints(60.toDuration(DurationUnit.SECONDS), "1m")
    }

    @Sample
    fun toDurationLong() {
        assertPrints(1_000_000_000L.toDuration(DurationUnit.NANOSECONDS), "1s")
        assertPrints(Long.MAX_VALUE.toDuration(DurationUnit.DAYS), "Infinity")
    }

    @Sample
    fun toDurationDouble() {
        assertPrints(1.5.toDuration(DurationUnit.HOURS), "1h 30m")
        assertPrints(0.001.toDuration(DurationUnit.SECONDS), "1ms")
    }

    @Sample
    fun timesOperatorInt() {
        val thirtyMinutes = 30.minutes

        assertPrints(2 * thirtyMinutes, "1h")
        assertPrints(3 * 10.seconds, "30s")
    }

    @Sample
    fun timesOperatorDouble() {
        val thirtyMinutes = 30.minutes

        assertPrints(1.5 * thirtyMinutes, "45m")
        assertPrints(0.5 * 1.hours, "30m")
    }

    @Sample
    fun durationUnitToTimeUnitConversion() {
        val timeUnit: java.util.concurrent.TimeUnit = DurationUnit.SECONDS.toTimeUnit()
        assertPrints(timeUnit, "SECONDS")
    }

    @Sample
    fun timeUnitToDurationUnitConversion() {
        val durationUnit: DurationUnit = java.util.concurrent.TimeUnit.MILLISECONDS.toDurationUnit()
        assertPrints(durationUnit, "MILLISECONDS")
    }

    @Sample
    fun toJavaDurationConversion() {
        val kotlinDuration = 5.5.seconds
        val javaDuration: java.time.Duration = kotlinDuration.toJavaDuration()
        assertPrints(javaDuration.toMillis(), "5500")
    }

    @Sample
    fun toKotlinDurationConversion() {
        val javaMinutes = java.time.Duration.ofMinutes(30)
        val kotlinMinutes: Duration = javaMinutes.toKotlinDuration()
        assertPrints(kotlinMinutes, "30m")
    }
}
