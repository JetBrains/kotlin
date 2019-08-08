/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseExperimental(ExperimentalTime::class)
package test.time

import test.numbers.assertAlmostEquals
import test.*
import kotlin.test.*
import kotlin.time.*
import kotlin.random.*

private val expectStorageUnit = DurationUnit.NANOSECONDS
private val units = DurationUnit.values()

class DurationTest {

    // construction white-box testing
    @Test
    fun construction() {

        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        repeat(100) {
            val value = Random.nextInt(1_000_000)
            val unit = units.random()
            val expected = convertDurationUnit(value.toDouble(), unit, expectStorageUnit)
            assertEquals(expected, value.toDuration(unit).value)
            assertEquals(expected, value.toLong().toDuration(unit).value)
            assertEquals(expected, value.toDouble().toDuration(unit).value)
        }

        todo {
            assertFails { Double.NaN.toDuration(DurationUnit.SECONDS) }
        }
    }

    @Test
    fun equality() {
        val data = listOf<Pair<Double, DurationUnit>>(
            Pair(2.0, DurationUnit.DAYS),
            Pair(2.0, DurationUnit.HOURS),
            Pair(0.25, DurationUnit.MINUTES),
            Pair(1.0, DurationUnit.SECONDS),
            Pair(50.0, DurationUnit.MILLISECONDS),
            Pair(0.3, DurationUnit.MICROSECONDS),
            Pair(20_000_000_000.0, DurationUnit.NANOSECONDS),
            Pair(1.0, DurationUnit.NANOSECONDS)
        )

        for ((value, unit) in data) {
            repeat(10) {
                val d1 = value.toDuration(unit)
                val unit2 = units.random()
                val value2 = Duration.convert(value, unit, unit2)
                val d2 = value2.toDuration(unit2)
                assertEquals(d1, d2, "$value $unit in $unit2")
                assertEquals(d1.hashCode(), d2.hashCode())

                val d3 = (value2 * 2).toDuration(unit2)
                assertNotEquals(d1, d3, "$value $unit in $unit2")
            }
        }
    }


    @Test
    fun conversionFromNumber() {
        val n1 = Random.nextInt(Int.MAX_VALUE)
        val n2 = Random.nextLong(Long.MAX_VALUE)
        val n3 = Random.nextDouble()

        assertEquals(n1.toDuration(DurationUnit.DAYS), n1.days)
        assertEquals(n2.toDuration(DurationUnit.DAYS), n2.days)
        assertEquals(n3.toDuration(DurationUnit.DAYS), n3.days)

        assertEquals(n1.toDuration(DurationUnit.HOURS), n1.hours)
        assertEquals(n2.toDuration(DurationUnit.HOURS), n2.hours)
        assertEquals(n3.toDuration(DurationUnit.HOURS), n3.hours)

        assertEquals(n1.toDuration(DurationUnit.MINUTES), n1.minutes)
        assertEquals(n2.toDuration(DurationUnit.MINUTES), n2.minutes)
        assertEquals(n3.toDuration(DurationUnit.MINUTES), n3.minutes)

        assertEquals(n1.toDuration(DurationUnit.SECONDS), n1.seconds)
        assertEquals(n2.toDuration(DurationUnit.SECONDS), n2.seconds)
        assertEquals(n3.toDuration(DurationUnit.SECONDS), n3.seconds)

        assertEquals(n1.toDuration(DurationUnit.MILLISECONDS), n1.milliseconds)
        assertEquals(n2.toDuration(DurationUnit.MILLISECONDS), n2.milliseconds)
        assertEquals(n3.toDuration(DurationUnit.MILLISECONDS), n3.milliseconds)

        assertEquals(n1.toDuration(DurationUnit.MICROSECONDS), n1.microseconds)
        assertEquals(n2.toDuration(DurationUnit.MICROSECONDS), n2.microseconds)
        assertEquals(n3.toDuration(DurationUnit.MICROSECONDS), n3.microseconds)

        assertEquals(n1.toDuration(DurationUnit.NANOSECONDS), n1.nanoseconds)
        assertEquals(n2.toDuration(DurationUnit.NANOSECONDS), n2.nanoseconds)
        assertEquals(n3.toDuration(DurationUnit.NANOSECONDS), n3.nanoseconds)
    }

    @Test
    fun conversionToNumber() {
        assertEquals(24.0, 1.days.inHours)
        assertEquals(0.5, 12.hours.inDays)
        assertEquals(15.0, 0.25.hours.inMinutes)
        assertEquals(600.0, 10.minutes.inSeconds)
        assertEquals(500.0, 0.5.seconds.inMilliseconds)
        assertEquals(50_000.0, 0.05.seconds.inMicroseconds)
        assertEquals(50_000.0, 0.05.milliseconds.inNanoseconds)

        assertEquals(365 * 86400e9, 365.days.inNanoseconds)

        assertEquals(0.0, Duration.ZERO.inNanoseconds)

        assertEquals(10500, 10.5.seconds.toLongMilliseconds())
        assertEquals(11, 11.5.milliseconds.toLongMilliseconds())
        assertEquals(-11, (-11.5).milliseconds.toLongMilliseconds())
        assertEquals(252_000_000, 252.milliseconds.toLongNanoseconds())
        assertEquals(Long.MAX_VALUE, (365.days * 293).toLongNanoseconds()) // clamping overflowed value

        repeat(100) {
            val value = Random.nextLong(1000)
            val unit = units.random()
            val unit2 = units.random()

            assertAlmostEquals(Duration.convert(value.toDouble(), unit, unit2), value.toDuration(unit).toDouble(unit2))
        }
    }

    @Test
    fun componentsOfProperSum() {
        repeat(100) {
            val h = Random.nextInt(24)
            val m = Random.nextInt(60)
            val s = Random.nextInt(60)
            val ns = Random.nextInt(1e9.toInt())
            (h.hours + m.minutes + s.seconds + ns.nanoseconds).run {
                toComponents { seconds, nanoseconds ->
                    assertEquals(h.toLong() * 3600 + m * 60 + s, seconds)
                    assertEquals(ns, nanoseconds)
                }
                toComponents { minutes, seconds, nanoseconds ->
                    assertEquals(h * 60 + m, minutes)
                    assertEquals(s, seconds)
                    assertEquals(ns, nanoseconds)
                }
                toComponents { hours, minutes, seconds, nanoseconds ->
                    assertEquals(h, hours)
                    assertEquals(m, minutes)
                    assertEquals(s, seconds)
                    assertEquals(ns, nanoseconds, "ns component of duration ${toIsoString()} differs too much, expected: $ns, actual: $nanoseconds")
                }
                toComponents { days, hours, minutes, seconds, nanoseconds ->
                    assertEquals(0, days)
                    assertEquals(h, hours)
                    assertEquals(m, minutes)
                    assertEquals(s, seconds)
                    assertEquals(ns, nanoseconds)
                }
            }
        }
    }

    @Test
    fun componentsOfCarriedSum() {
        (36.hours + 90.minutes + 90.seconds + 1500.milliseconds).run {
            toComponents { days, hours, minutes, seconds, nanoseconds ->
                assertEquals(1, days)
                assertEquals(13, hours)
                assertEquals(31, minutes)
                assertEquals(31, seconds)
                assertEquals(500_000_000, nanoseconds)
            }
        }
    }

    @Test
    fun infinite() {
        assertTrue(Duration.INFINITE.isInfinite())
        assertTrue((-Duration.INFINITE).isInfinite())
        assertTrue(Double.POSITIVE_INFINITY.nanoseconds.isInfinite())

        // seconds converted to nanoseconds overflow to infinite
        assertTrue(Double.MAX_VALUE.seconds.isInfinite())
        assertTrue((-Double.MAX_VALUE).seconds.isInfinite())
    }


    @Test
    fun negation() {
        repeat(100) {
            val value = Random.nextLong()
            val unit = units.random()

            assertEquals((-value).toDuration(unit), -value.toDuration(unit))
        }
    }

    @Test
    fun signAndAbsoluteValue() {
        val negative = -1.seconds
        val positive = 1.seconds
        val zero = Duration.ZERO

        assertTrue(negative.isNegative())
        assertFalse(zero.isNegative())
        assertFalse(positive.isNegative())

        assertFalse(negative.isPositive())
        assertFalse(zero.isPositive())
        assertTrue(positive.isPositive())

        assertEquals(positive, negative.absoluteValue)
        assertEquals(positive, positive.absoluteValue)
        assertEquals(zero, zero.absoluteValue)
    }


    @Test
    fun addition() {
        assertEquals(1.5.hours, 1.hours + 30.minutes)
        assertEquals(0.5.days, 6.hours + 360.minutes)
        assertEquals(0.5.seconds, 200.milliseconds + 300_000.microseconds)
    }

    @Test
    fun subtraction() {
        assertEquals(10.hours, 0.5.days - 120.minutes)
        assertEquals(850.milliseconds, 1.seconds - 150.milliseconds)
        // TODO decide on INFINITE - INFINITE
    }

    @Test
    fun multiplication() {
        assertEquals(1.days, 12.hours * 2)
        assertEquals(1.days, 60.minutes * 24.0)
        assertEquals(1.microseconds, 20.nanoseconds * 50)

        assertEquals(1.days, 2 * 12.hours)
        assertEquals(12.5.hours, 12.5 * 60.minutes)
        assertEquals(1.microseconds, 50 * 20.nanoseconds)
    }

    @Test
    fun divisionByNumber() {
        assertEquals(12.hours, 1.days / 2)
        assertEquals(60.minutes, 1.days / 24.0)
        assertEquals(20.seconds, 2.minutes / 6)
    }

    @Test
    fun divisionByDuration() {
        assertEquals(24.0, 1.days / 1.hours)
        assertEquals(0.1, 9.minutes / 1.5.hours)
        assertEquals(50.0, 1.microseconds / 20.nanoseconds)
    }

    @Test
    fun toIsoString() {
        // zero
        assertEquals("PT0S", Duration.ZERO.toIsoString())

        // single unit
        assertEquals("PT24H", 1.days.toIsoString())
        assertEquals("PT1H", 1.hours.toIsoString())
        assertEquals("PT1M", 1.minutes.toIsoString())
        assertEquals("PT1S", 1.seconds.toIsoString())
        assertEquals("PT0.001S", 1.milliseconds.toIsoString())
        assertEquals("PT0.000001S", 1.microseconds.toIsoString())
        assertEquals("PT0.000000001S", 1.nanoseconds.toIsoString())

        // rounded to zero
        assertEquals("PT0S", 0.1.nanoseconds.toIsoString())
        assertEquals("PT0S", 0.9.nanoseconds.toIsoString())

        // several units combined
        assertEquals("PT24H1M", (1.days + 1.minutes).toIsoString())
        assertEquals("PT24H0M1S", (1.days + 1.seconds).toIsoString())
        assertEquals("PT24H0M0.001S", (1.days + 1.milliseconds).toIsoString())
        assertEquals("PT1H30M", (1.hours + 30.minutes).toIsoString())
        assertEquals("PT1H0M0.500S", (1.hours + 500.milliseconds).toIsoString())
        assertEquals("PT2M0.500S", (2.minutes + 500.milliseconds).toIsoString())
        assertEquals("PT1M30.500S", (90_500.milliseconds).toIsoString())

        // negative
        assertEquals("-PT23H45M", (-1.days + 15.minutes).toIsoString())
        assertEquals("-PT24H15M", (-1.days - 15.minutes).toIsoString())

        // infinite
        assertEquals("PT2147483647H", Duration.INFINITE.toIsoString())
    }

    @Test
    fun toStringInUnits() {
        var d = 1.days + 15.hours + 31.minutes + 45.seconds + 678.milliseconds + 920.microseconds + 516.34.nanoseconds

        fun test(unit: DurationUnit, vararg representations: String) {
            assertFails { d.toString(unit, -1) }
            assertEquals(representations.toList(), representations.indices.map { d.toString(unit, it) })
        }

        test(DurationUnit.DAYS, "2d", "1.6d", "1.65d", "1.647d")
        test(DurationUnit.HOURS, "40h", "39.5h", "39.53h")
        test(DurationUnit.MINUTES, "2372m", "2371.8m", "2371.76m")
        d -= 39.hours
        test(DurationUnit.SECONDS, "1906s", "1905.7s", "1905.68s", "1905.679s")
        d -= 1904.seconds
        test(DurationUnit.MILLISECONDS, "1679ms", "1678.9ms", "1678.92ms", "1678.921ms")
        d -= 1678.milliseconds
        test(DurationUnit.MICROSECONDS, "921us", "920.5us", "920.52us", "920.516us")
        d -= 920.microseconds
        // sub-nanosecond precision errors
        test(DurationUnit.NANOSECONDS, "516ns", "516.3ns", "516.34ns", "516.344ns", "516.3438ns")
        d = (d - 516.nanoseconds) / 17
        test(DurationUnit.NANOSECONDS, "0ns", "0.0ns", "0.02ns", "0.020ns", "0.0202ns")

        d = Double.MAX_VALUE.nanoseconds
        test(DurationUnit.DAYS, "2.08e+294d")
        test(DurationUnit.NANOSECONDS, "1.80e+308ns")

        assertEquals("0.500000000000s", 0.5.seconds.toString(DurationUnit.SECONDS, 100))
        assertEquals("99999000000000.000000000000ns", 99_999.seconds.toString(DurationUnit.NANOSECONDS, 15))
        assertEquals("1.00e+14ns", 100_000.seconds.toString(DurationUnit.NANOSECONDS, 9))

        d = Duration.INFINITE
        test(DurationUnit.DAYS, "Infinity", "Infinity")
        d = -Duration.INFINITE
        test(DurationUnit.NANOSECONDS, "-Infinity", "-Infinity")
    }


    @Test
    fun toStringDefault() {
        fun test(duration: Duration, vararg expectedOptions: String) {
            val actual = duration.toString()

            if (!expectedOptions.contains(actual)) {
                assertEquals<Any>(expectedOptions.toList(), duration.toString())
            }
            if (duration > Duration.ZERO)
                assertEquals("-$actual", (-duration).toString())
        }

        test(101.days, "101d")
        test(45.3.days, "45.3d")
        test(45.days, "45.0d")

        test(40.5.days, "972h")
        test(40.hours + 15.minutes, "40.3h", "40.2h")
        test(40.hours, "40.0h")

        test(12.5.hours, "750m")
        test(30.minutes, "30.0m")
        test(17.5.minutes, "17.5m")

        test(16.5.minutes, "990s")
        test(90.36.seconds, "90.4s")
        test(50.seconds, "50.0s")
        test(1.3.seconds, "1.30s")
        test(1.seconds, "1.00s")

        test(0.5.seconds, "500ms")
        test(40.2.milliseconds, "40.2ms")
        test(4.225.milliseconds, "4.23ms", "4.22ms")
        test(4.245.milliseconds, "4.25ms")
        test(1.milliseconds, "1.00ms")

        test(0.75.milliseconds, "750us")
        test(75.35.microseconds, "75.4us", "75.3us")
        test(7.25.microseconds, "7.25us")
        test(1.035.microseconds, "1.04us", "1.03us")
        test(1.005.microseconds, "1.01us", "1.00us")

        test(950.5.nanoseconds, "951ns", "950ns")
        test(85.23.nanoseconds, "85.2ns")
        test(8.235.nanoseconds, "8.24ns", "8.23ns")
        test(1.3.nanoseconds, "1.30ns")

        test(0.75.nanoseconds, "0.75ns")
        test(0.7512.nanoseconds, "0.7512ns")
        test(0.023.nanoseconds, "0.023ns")
        test(0.0034.nanoseconds, "0.0034ns")
        test(0.0000035.nanoseconds, "0.0000035ns")

        test(Duration.ZERO, "0s")
        test(365.days * 10000, "3650000d")
        test(300.days * 100000, "3.00e+7d")
        test(365.days * 100000, "3.65e+7d")

        val universeAge = 365.25.days * 13.799e9
        val planckTime = 5.4e-44.seconds

        test(universeAge, "5.04e+12d")
        test(planckTime, "5.40e-44s")
        test(Double.MAX_VALUE.nanoseconds, "2.08e+294d")
        test(Duration.INFINITE, "Infinity")
    }

}