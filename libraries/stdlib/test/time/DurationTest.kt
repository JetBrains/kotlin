/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class)
@file:Suppress("INVISIBLE_MEMBER")
package test.time

import test.numbers.assertAlmostEquals
import kotlin.native.concurrent.SharedImmutable
import kotlin.test.*
import kotlin.time.*
import kotlin.random.*

@SharedImmutable
private val units = DurationUnit.values()

class DurationTest {

    @Test
    fun construction() {
        // nanosecond precision
        val testValues = listOf(0L, 1L, Long.MAX_VALUE / 2) + List(100) { Random.nextLong(0, Long.MAX_VALUE / 2) }
        for (value in testValues) {
            assertEquals(value, value.toDuration(DurationUnit.NANOSECONDS).toLongNanoseconds())
            assertEquals(-value, -value.toDuration(DurationUnit.NANOSECONDS).toLongNanoseconds())
        }
        // expressible as long nanoseconds but stored as milliseconds
        for (delta in testValues) {
            val value = 1L.shl(62) + delta
            val expected = value - (value % 1_000_000)
            assertEquals(expected, value.toDuration(DurationUnit.NANOSECONDS).toLongNanoseconds())
            assertEquals(-expected, -value.toDuration(DurationUnit.NANOSECONDS).toLongNanoseconds())
        }
        // any int value of small units can always be represented in nanoseconds
        for (unit in units.filter { it <= DurationUnit.SECONDS }) {
            val scale = convertDurationUnitOverflow(1L, unit, DurationUnit.NANOSECONDS)
            repeat(100) {
                val value = Random.nextInt()
                assertEquals(value * scale, value.toDuration(unit).toLongNanoseconds())
            }
        }

        assertEquals(Long.MAX_VALUE / 1000, Long.MAX_VALUE.toDuration(DurationUnit.MICROSECONDS).toLongMilliseconds())
        assertEquals(Long.MAX_VALUE / 1000 * 1000, Long.MAX_VALUE.toDuration(DurationUnit.MICROSECONDS).toLong(DurationUnit.MICROSECONDS))

        assertEquals(Duration.INFINITE, (Long.MAX_VALUE / 2).toDuration(DurationUnit.MILLISECONDS))
        assertEquals(-Duration.INFINITE, (Long.MIN_VALUE / 2).toDuration(DurationUnit.MILLISECONDS))

        assertFailsWith<IllegalArgumentException> { Double.NaN.toDuration(DurationUnit.SECONDS) }
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
    fun comparison() {
        run {
            val d1 = Duration.nanoseconds(Long.MAX_VALUE / 2 + 1)
            val d2 = Duration.nanoseconds(Long.MAX_VALUE / 2)
            assertTrue(d1 >= d2, "same sign, different ranges")
            // TODO: Address this contradiction
//            assertTrue(d1.toLongNanoseconds() >= d2.toLongNanoseconds())

            val d3 = Duration.nanoseconds(Long.MAX_VALUE / 2 - 1)
            assertTrue(d2 > d3, "same sign, same range nanos")

            val d4 = Duration.nanoseconds(Long.MAX_VALUE)
            assertTrue(d4 > d1, "same sign, same range millis")

            assertTrue(d2 > -d1, "different signs, different ranges")
            assertTrue(d1 > -d4, "different signs, same ranges")
        }
    }


    @Test
    fun conversionFromNumber() {
        val n1 = Random.nextInt(Int.MAX_VALUE)
        val n2 = Random.nextLong(Long.MAX_VALUE)
        val n3 = Random.nextDouble()

        assertEquals(n1.toDuration(DurationUnit.DAYS), Duration.days(n1))
        assertEquals(n2.toDuration(DurationUnit.DAYS), Duration.days(n2))
        assertEquals(n3.toDuration(DurationUnit.DAYS), Duration.days(n3))

        assertEquals(n1.toDuration(DurationUnit.HOURS), Duration.hours(n1))
        assertEquals(n2.toDuration(DurationUnit.HOURS), Duration.hours(n2))
        assertEquals(n3.toDuration(DurationUnit.HOURS), Duration.hours(n3))

        assertEquals(n1.toDuration(DurationUnit.MINUTES), Duration.minutes(n1))
        assertEquals(n2.toDuration(DurationUnit.MINUTES), Duration.minutes(n2))
        assertEquals(n3.toDuration(DurationUnit.MINUTES), Duration.minutes(n3))

        assertEquals(n1.toDuration(DurationUnit.SECONDS), Duration.seconds(n1))
        assertEquals(n2.toDuration(DurationUnit.SECONDS), Duration.seconds(n2))
        assertEquals(n3.toDuration(DurationUnit.SECONDS), Duration.seconds(n3))

        assertEquals(n1.toDuration(DurationUnit.MILLISECONDS), Duration.milliseconds(n1))
        assertEquals(n2.toDuration(DurationUnit.MILLISECONDS), Duration.milliseconds(n2))
        assertEquals(n3.toDuration(DurationUnit.MILLISECONDS), Duration.milliseconds(n3))

        assertEquals(n1.toDuration(DurationUnit.MICROSECONDS), Duration.microseconds(n1))
        assertEquals(n2.toDuration(DurationUnit.MICROSECONDS), Duration.microseconds(n2))
        assertEquals(n3.toDuration(DurationUnit.MICROSECONDS), Duration.microseconds(n3))

        assertEquals(n1.toDuration(DurationUnit.NANOSECONDS), Duration.nanoseconds(n1))
        assertEquals(n2.toDuration(DurationUnit.NANOSECONDS), Duration.nanoseconds(n2))
        assertEquals(n3.toDuration(DurationUnit.NANOSECONDS), Duration.nanoseconds(n3))
    }

    @Test
    fun conversionToNumber() {
        assertEquals(24.0, Duration.days(1).inHours)
        assertEquals(0.5, Duration.hours(12).inDays)
        assertEquals(15.0, Duration.hours(0.25).inMinutes)
        assertEquals(600.0, Duration.minutes(10).inSeconds)
        assertEquals(500.0, Duration.seconds(0.5).inMilliseconds)
        assertEquals(50_000.0, Duration.seconds(0.05).inMicroseconds)
        assertEquals(50_000.0, Duration.milliseconds(0.05).inNanoseconds)

        assertEquals(365 * 86400e9, Duration.days(365).inNanoseconds)

        assertEquals(0.0, Duration.ZERO.inNanoseconds)

        assertEquals(10500, Duration.seconds(10.5).toLongMilliseconds())
        assertEquals(11, Duration.milliseconds(11.5).toLongMilliseconds())
        assertEquals(-11, Duration.milliseconds((-11.5)).toLongMilliseconds())
        assertEquals(252_000_000, Duration.milliseconds(252).toLongNanoseconds())
        assertEquals(Long.MAX_VALUE, (Duration.days(365) * 293).toLongNanoseconds()) // clamping overflowed value

        repeat(100) {
            val value = Random.nextLong(1000)
            val unit = units.random()
            val unit2 = units.random()

            assertAlmostEquals(Duration.convert(value.toDouble(), unit, unit2), value.toDuration(unit).toDouble(unit2))
        }

        for (unit in units) {
            assertEquals(Long.MAX_VALUE, Duration.INFINITE.toLong(unit))
            assertEquals(Int.MAX_VALUE, Duration.INFINITE.toInt(unit))
            assertEquals(Double.POSITIVE_INFINITY, Duration.INFINITE.toDouble(unit))
            assertEquals(Long.MIN_VALUE, (-Duration.INFINITE).toLong(unit))
            assertEquals(Int.MIN_VALUE, (-Duration.INFINITE).toInt(unit))
            assertEquals(Double.NEGATIVE_INFINITY, (-Duration.INFINITE).toDouble(unit))
        }
    }

    @Test
    fun componentsOfProperSum() {
        repeat(100) {
            val h = Random.nextInt(24)
            val m = Random.nextInt(60)
            val s = Random.nextInt(60)
            val ns = Random.nextInt(1e9.toInt())
            (Duration.hours(h) + Duration.minutes(m) + Duration.seconds(s) + Duration.nanoseconds(ns)).run {
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
        (Duration.hours(36) + Duration.minutes(90) + Duration.seconds(90) + Duration.milliseconds(1500)).run {
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
        assertTrue(Duration.nanoseconds(Double.POSITIVE_INFINITY).isInfinite())

        // seconds converted to nanoseconds overflow to infinite
        assertTrue(Duration.seconds(Double.MAX_VALUE).isInfinite())
        assertTrue(Duration.seconds((-Double.MAX_VALUE)).isInfinite())
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
        val negative = -Duration.seconds(1)
        val positive = Duration.seconds(1)
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
    fun negativeZero() {
        fun equivalentToZero(value: Duration) {
            assertEquals(Duration.ZERO, value)
            assertEquals(Duration.ZERO, value.absoluteValue)
            assertEquals(value, value.absoluteValue)
            assertEquals(value, value.absoluteValue)
            assertFalse(value.isNegative())
            assertFalse(value.isPositive())
            assertEquals(Duration.ZERO.toString(), value.toString())
            assertEquals(Duration.ZERO.toIsoString(), value.toIsoString())
            assertEquals(Duration.ZERO.inSeconds, value.inSeconds)
            assertEquals(0, Duration.ZERO.compareTo(value))
            assertEquals(0, Duration.ZERO.inNanoseconds.compareTo(value.inNanoseconds))
        }
        equivalentToZero(Duration.seconds(-0.0))
        equivalentToZero((-0.0).toDuration(DurationUnit.DAYS))
        equivalentToZero(-Duration.ZERO)
        equivalentToZero(Duration.seconds(-1) / Double.POSITIVE_INFINITY)
        equivalentToZero(Duration.seconds(0) / -1)
        equivalentToZero(Duration.seconds(-1) * 0.0)
        equivalentToZero(Duration.seconds(0) * -1)
    }


    @Test
    fun addition() {
        assertEquals(Duration.hours(1.5), Duration.hours(1) + Duration.minutes(30))
        assertEquals(Duration.days(0.5), Duration.hours(6) + Duration.minutes(360))
        assertEquals(Duration.seconds(0.5), Duration.milliseconds(200) + Duration.microseconds(300_000))

        for (value in listOf(Duration.ZERO, Duration.nanoseconds(1), Duration.days(500 * 365))) {
            for (inf in listOf(Duration.INFINITE, -Duration.INFINITE)) {
                for (result in listOf(inf + inf, inf + value, inf + (-value), value + inf, (-value) + inf)) {
                    assertEquals(inf, result)
                }
            }
        }

        assertFailsWith<IllegalArgumentException> { Duration.INFINITE + (-Duration.INFINITE) }
    }

    @Test
    fun subtraction() {
        assertEquals(Duration.hours(10), Duration.days(0.5) - Duration.minutes(120))
        assertEquals(Duration.milliseconds(850), Duration.seconds(1) - Duration.milliseconds(150))
        assertEquals(Duration.milliseconds(1150), Duration.seconds(1) - Duration.milliseconds(-150))
        assertEquals(Duration.milliseconds(1), Duration.microseconds(Long.MAX_VALUE) - Duration.microseconds(Long.MAX_VALUE - 1_000))

        run {
            val offset = 2_000_000L
            val value = Long.MAX_VALUE / 2 + offset
            val base = Duration.nanoseconds(value)
            val baseNs = base.toLongMilliseconds() * 1_000_000
            assertEquals(baseNs, base.toLongNanoseconds())  // base stored as millis

            val smallDeltas = listOf(1L, 2L, 1000L, offset / 2 - 1) + List(10) { Random.nextLong(offset / 2) }
            for (smallDeltaNs in smallDeltas) {
                assertEquals(base, base - Duration.nanoseconds(smallDeltaNs), "delta: $smallDeltaNs")
            }

            val deltas = listOf(offset + 1L, offset + 1500L) +
                    List(10) { Random.nextLong(offset + 1500, offset + 10000) } +
                    List(100) { Random.nextLong(offset + 1500, Long.MAX_VALUE / 2) }
            for (deltaNs in deltas) {
                val delta = Duration.nanoseconds(deltaNs)
                assertEquals(deltaNs, delta.toLongNanoseconds())
                assertEquals(baseNs - deltaNs, (base - delta).toLongNanoseconds(), "base: $baseNs, delta: $deltaNs")
            }
        }

        for (value in listOf(Duration.ZERO, Duration.nanoseconds(1), Duration.days(500 * 365))) {
            for (inf in listOf(Duration.INFINITE, -Duration.INFINITE)) {
                for (result in listOf(inf - (-inf), inf - value, inf - (-value), value - (-inf), (-value) - (-inf))) {
                    assertEquals(inf, result)
                }
            }
        }

        assertFailsWith<IllegalArgumentException> { Duration.INFINITE - Duration.INFINITE }
    }

    @Test
    fun multiplication() {
        assertEquals(Duration.days(1), Duration.hours(12) * 2)
        assertEquals(Duration.days(1), Duration.minutes(60) * 24.0)
        assertEquals(Duration.microseconds(1), Duration.nanoseconds(20) * 50)

        assertEquals(Duration.days(1), 2 * Duration.hours(12))
        assertEquals(Duration.hours(12.5), 12.5 * Duration.minutes(60))
        assertEquals(Duration.microseconds(1), 50 * Duration.nanoseconds(20))

        assertEquals(Duration.ZERO, 0 * Duration.hours(1))
        assertEquals(Duration.ZERO, Duration.seconds(1) * 0.0)

        assertEquals(Duration.INFINITE, Duration.INFINITE * Double.POSITIVE_INFINITY)
        assertEquals(Duration.INFINITE, Duration.INFINITE * Double.MIN_VALUE)
        assertEquals(-Duration.INFINITE, Duration.INFINITE * Double.NEGATIVE_INFINITY)
        assertEquals(-Duration.INFINITE, Duration.INFINITE * -1)

        assertFailsWith<IllegalArgumentException> { Duration.INFINITE * 0 }
        assertFailsWith<IllegalArgumentException> { 0.0 * Duration.INFINITE }
    }

    @Test
    fun divisionByNumber() {
        assertEquals(Duration.hours(12), Duration.days(1) / 2)
        assertEquals(Duration.minutes(60), Duration.days(1) / 24.0)
        assertEquals(Duration.seconds(20), Duration.minutes(2) / 6)

        assertEquals(Duration.INFINITE, Duration.seconds(1) / 0)
        assertEquals(-Duration.INFINITE, -Duration.seconds(1) / 0.0)
        assertEquals(Duration.INFINITE, -Duration.seconds(1) / (-0.0))

        assertEquals(Duration.INFINITE, Duration.INFINITE / Int.MAX_VALUE)
        assertEquals(Duration.INFINITE, -Duration.INFINITE / Int.MIN_VALUE)
        assertEquals(-Duration.INFINITE, Duration.INFINITE / -1)
        assertEquals(Duration.INFINITE, Duration.INFINITE / Double.MAX_VALUE)

        assertFailsWith<IllegalArgumentException> { Duration.INFINITE / Double.POSITIVE_INFINITY }
        assertFailsWith<IllegalArgumentException> { Duration.ZERO / 0 }
        assertFailsWith<IllegalArgumentException> { Duration.ZERO / 0.0 }
    }

    @Test
    fun divisionByDuration() {
        assertEquals(24.0, Duration.days(1) / Duration.hours(1))
        assertEquals(0.1, Duration.minutes(9) / Duration.hours(1.5))
        assertEquals(50.0, Duration.microseconds(1) / Duration.nanoseconds(20))
        assertEquals(299.0, Duration.days(365 * 299) / Duration.days(365))

        assertTrue((Duration.INFINITE / Duration.INFINITE).isNaN())
    }

    @Test
    fun toIsoString() {
        // zero
        assertEquals("PT0S", Duration.ZERO.toIsoString())

        // single unit
        assertEquals("PT24H", Duration.days(1).toIsoString())
        assertEquals("PT1H", Duration.hours(1).toIsoString())
        assertEquals("PT1M", Duration.minutes(1).toIsoString())
        assertEquals("PT1S", Duration.seconds(1).toIsoString())
        assertEquals("PT0.001S", Duration.milliseconds(1).toIsoString())
        assertEquals("PT0.000001S", Duration.microseconds(1).toIsoString())
        assertEquals("PT0.000000001S", Duration.nanoseconds(1).toIsoString())

        // rounded to zero
        assertEquals("PT0S", Duration.nanoseconds(0.1).toIsoString())
        assertEquals("PT0S", Duration.nanoseconds(0.9).toIsoString())

        // several units combined
        assertEquals("PT24H1M", (Duration.days(1) + Duration.minutes(1)).toIsoString())
        assertEquals("PT24H0M1S", (Duration.days(1) + Duration.seconds(1)).toIsoString())
        assertEquals("PT24H0M0.001S", (Duration.days(1) + Duration.milliseconds(1)).toIsoString())
        assertEquals("PT1H30M", (Duration.hours(1) + Duration.minutes(30)).toIsoString())
        assertEquals("PT1H0M0.500S", (Duration.hours(1) + Duration.milliseconds(500)).toIsoString())
        assertEquals("PT2M0.500S", (Duration.minutes(2) + Duration.milliseconds(500)).toIsoString())
        assertEquals("PT1M30.500S", (Duration.milliseconds(90_500)).toIsoString())

        // negative
        assertEquals("-PT23H45M", (-Duration.days(1) + Duration.minutes(15)).toIsoString())
        assertEquals("-PT24H15M", (-Duration.days(1) - Duration.minutes(15)).toIsoString())

        // infinite
        assertEquals("PT2147483647H", Duration.INFINITE.toIsoString())
    }

    @Test
    fun toStringInUnits() {
        var d = with(Duration) {
            days(1) + hours(15) + minutes(31) + seconds(45) +
            milliseconds(678) + microseconds(920) + nanoseconds(516.34)
        }

        fun test(unit: DurationUnit, vararg representations: String) {
            assertFails { d.toString(unit, -1) }
            assertEquals(representations.toList(), representations.indices.map { d.toString(unit, it) })
        }

        test(DurationUnit.DAYS, "2d", "1.6d", "1.65d", "1.647d")
        test(DurationUnit.HOURS, "40h", "39.5h", "39.53h")
        test(DurationUnit.MINUTES, "2372m", "2371.8m", "2371.76m")
        d -= Duration.hours(39)
        test(DurationUnit.SECONDS, "1906s", "1905.7s", "1905.68s", "1905.679s")
        d -= Duration.seconds(1904)
        test(DurationUnit.MILLISECONDS, "1679ms", "1678.9ms", "1678.92ms", "1678.921ms")
        d -= Duration.milliseconds(1678)
        test(DurationUnit.MICROSECONDS, "921us", "920.5us", "920.52us", "920.516us")
        d -= Duration.microseconds(920)
        // no sub-nanosecond precision
        test(DurationUnit.NANOSECONDS, "516ns", "516.0ns", "516.00ns", "516.000ns", "516.0000ns")
        d = (d - Duration.nanoseconds(516)) / 17
        test(DurationUnit.NANOSECONDS, "0ns", "0.0ns", "0.00ns", "0.000ns", "0.0000ns")

        // infinite
//        d = Duration.nanoseconds(Double.MAX_VALUE)
//        test(DurationUnit.DAYS, "2.08e+294d")
//        test(DurationUnit.NANOSECONDS, "1.80e+308ns")

        assertEquals("0.500000000000s", Duration.seconds(0.5).toString(DurationUnit.SECONDS, 100))
        assertEquals("99999000000000.000000000000ns", Duration.seconds(99_999).toString(DurationUnit.NANOSECONDS, 15))
        assertEquals("1.00e+14ns", Duration.seconds(100_000).toString(DurationUnit.NANOSECONDS, 9))

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

        test(Duration.days(101), "101d")
        test(Duration.days(45.3), "45.3d")
        test(Duration.days(45), "45.0d")

        test(Duration.days(40.5), "972h")
        test(Duration.hours(40) + Duration.minutes(15), "40.3h", "40.2h")
        test(Duration.hours(40), "40.0h")

        test(Duration.hours(12.5), "750m")
        test(Duration.minutes(30), "30.0m")
        test(Duration.minutes(17.5), "17.5m")

        test(Duration.minutes(16.5), "990s")
        test(Duration.seconds(90.36), "90.4s")
        test(Duration.seconds(50), "50.0s")
        test(Duration.seconds(1.3), "1.30s")
        test(Duration.seconds(1), "1.00s")

        test(Duration.seconds(0.5), "500ms")
        test(Duration.milliseconds(40.2), "40.2ms")
        test(Duration.milliseconds(4.225), "4.23ms", "4.22ms")
        test(Duration.milliseconds(4.245), "4.25ms")
        test(Duration.milliseconds(1), "1.00ms")

        test(Duration.milliseconds(0.75), "750us")
        test(Duration.microseconds(75.35), "75.4us", "75.3us")
        test(Duration.microseconds(7.25), "7.25us")
        test(Duration.microseconds(1.035), "1.04us", "1.03us")
        test(Duration.microseconds(1.005), "1.01us", "1.00us")

        test(Duration.nanoseconds(950.5), "951ns", "950ns")
        test(Duration.nanoseconds(85.23), "85.0ns")
        test(Duration.nanoseconds(8.235), "8.00ns")
        test(Duration.nanoseconds(1.3), "1.00ns")

        // equal to zero
//        test(Duration.nanoseconds(0.75), "0.75ns")
//        test(Duration.nanoseconds(0.7512), "0.7512ns")
//        test(Duration.nanoseconds(0.023), "0.023ns")
//        test(Duration.nanoseconds(0.0034), "0.0034ns")
//        test(Duration.nanoseconds(0.0000035), "0.0000035ns")

        test(Duration.ZERO, "0s")
        test(Duration.days(365) * 10000, "3650000d")
        test(Duration.days(300) * 100000, "3.00e+7d")
        test(Duration.days(365) * 100000, "3.65e+7d")

        // all infinite
//        val universeAge = Duration.days(365.25) * 13.799e9
//        val planckTime = Duration.seconds(5.4e-44)

//        test(universeAge, "5.04e+12d")
//        test(planckTime, "5.40e-44s")
//        test(Duration.nanoseconds(Double.MAX_VALUE), "2.08e+294d")
        test(Duration.INFINITE, "Infinity")
    }

}
