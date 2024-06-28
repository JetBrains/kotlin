/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER")
package test.time

import test.TestPlatform
import test.current
import test.numbers.assertAlmostEquals
import kotlin.math.nextDown
import kotlin.math.pow
import kotlin.test.*
import kotlin.time.*
import kotlin.random.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private val units = DurationUnit.entries

class DurationTest {

    @Test
    fun constructionFromNumber() {
        // nanosecond precision
        val testValues = listOf(0L, 1L, MAX_NANOS) + List(100) { Random.nextLong(0, MAX_NANOS) }
        for (value in testValues) {
            assertEquals(value, value.toDuration(DurationUnit.NANOSECONDS).inWholeNanoseconds)
            assertEquals(-value, -value.toDuration(DurationUnit.NANOSECONDS).inWholeNanoseconds)
        }
        // expressible as long nanoseconds but stored as milliseconds
        for (delta in testValues) {
            val value = (MAX_NANOS + 1) + delta
            val expected = value - (value % NANOS_IN_MILLIS)
            assertEquals(expected, value.toDuration(DurationUnit.NANOSECONDS).inWholeNanoseconds)
            assertEquals(-expected, -value.toDuration(DurationUnit.NANOSECONDS).inWholeNanoseconds)
        }
        // any int value of small units can always be represented in nanoseconds
        for (unit in units.filter { it <= DurationUnit.SECONDS }) {
            val scale = convertDurationUnitOverflow(1L, unit, DurationUnit.NANOSECONDS)
            repeat(100) {
                val value = Random.nextInt()
                assertEquals(value * scale, value.toDuration(unit).inWholeNanoseconds)
            }
        }

        for (unit in units) {
            val borderValue = convertDurationUnit(MAX_NANOS, DurationUnit.NANOSECONDS, unit)
            val d1 = borderValue.toDuration(unit)
            val d2 = (borderValue + 1).toDuration(unit)
            assertNotEquals(d1, d1 + 1.nanoseconds)
            assertEquals(d2, d2 + 1.nanoseconds)
        }

        assertEquals(Long.MAX_VALUE / 1000, Long.MAX_VALUE.toDuration(DurationUnit.MICROSECONDS).inWholeMilliseconds)
        assertEquals(Long.MAX_VALUE / 1000 * 1000, Long.MAX_VALUE.toDuration(DurationUnit.MICROSECONDS).toLong(DurationUnit.MICROSECONDS))

        assertEquals(Duration.INFINITE, (MAX_MILLIS).toDuration(DurationUnit.MILLISECONDS))
        assertEquals(-Duration.INFINITE, (-MAX_MILLIS).toDuration(DurationUnit.MILLISECONDS))

        run {
            val maxNsDouble = MAX_NANOS.toDouble()
            val lessThanMaxDouble = maxNsDouble.nextDown()
            val maxNs = maxNsDouble.toDuration(DurationUnit.NANOSECONDS).inWholeNanoseconds
            val lessThanMaxNs = lessThanMaxDouble.toDuration(DurationUnit.NANOSECONDS).inWholeNanoseconds
            assertTrue(maxNs > lessThanMaxNs, "$maxNs should be > $lessThanMaxNs")
        }

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
                @OptIn(ExperimentalTime::class)
                val value2 = Duration.convert(value, unit, unit2)
                val d2 = value2.toDuration(unit2)
                assertEquals(d1, d2, "$value $unit in $unit2")
                assertEquals(d1.hashCode(), d2.hashCode())

                val d3 = (value2 * 2).toDuration(unit2)
                assertNotEquals(d1, d3, "$value $unit in $unit2")
            }
        }

        run { // invariant Duration.nanoseconds(d.inWholeNanoseconds) == d when whole nanoseconds fits into Long range
            val d1 = (MAX_NANOS + 1).nanoseconds
            val d2 = d1.inWholeNanoseconds.nanoseconds
            assertEquals(d1.inWholeNanoseconds, d2.inWholeNanoseconds)
            assertEquals(d1, d2)
        }
    }

    @Test
    fun comparison() {
        fun assertGreater(d1: Duration, d2: Duration, message: String) {
            assertTrue(d1 > d2, message)
            assertFalse(d1 <= d2, message)
            assertTrue(
                d1.inWholeNanoseconds > d2.inWholeNanoseconds ||
                        d1.inWholeNanoseconds == d2.inWholeNanoseconds && d1.inWholeMilliseconds > d2.inWholeMilliseconds,
                message
            )
        }

        val d4 = Long.MAX_VALUE.nanoseconds
        val d3 = (MAX_NANOS + 1).nanoseconds
        val d2 = MAX_NANOS.nanoseconds
        val d1 = (MAX_NANOS - 1).nanoseconds

        assertGreater(d4, d2, "same sign, different ranges")
        assertGreater(d3, d2, "same sign, different ranges 2")
        assertGreater(d2, d1, "same sign, same range nanos")
        assertGreater(d4, d3, "same sign, same range millis")
        assertGreater(d2, -d3, "different signs, different ranges")
        assertGreater(d3, -d4, "different signs, same ranges")
        assertGreater(d1, -d2, "different signs, same ranges 2")
    }


    @Test
    fun constructionFactoryFunctions() {
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
        assertEquals(24, 1.days.inWholeHours)
        assertEquals(0.5, 12.hours.toDouble(DurationUnit.DAYS))
        assertEquals(0, 12.hours.inWholeDays)
        assertEquals(15, 0.25.hours.inWholeMinutes)
        assertEquals(600, 10.minutes.inWholeSeconds)
        assertEquals(500, 0.5.seconds.inWholeMilliseconds)
        assertEquals(50_000, 0.05.seconds.inWholeMicroseconds)
        assertEquals(50_000, 0.05.milliseconds.inWholeNanoseconds)

        assertEquals(365 * 86400 * 1_000_000_000L, 365.days.inWholeNanoseconds)

        assertEquals(0, Duration.ZERO.inWholeNanoseconds)
        assertEquals(0, Duration.ZERO.inWholeMicroseconds)
        assertEquals(0, Duration.ZERO.inWholeMilliseconds)

        assertEquals(10500, 10.5.seconds.inWholeMilliseconds)
        assertEquals(11, 11.5.milliseconds.inWholeMilliseconds)
        assertEquals(-11, (-11.5).milliseconds.inWholeMilliseconds)
        assertEquals(252_000_000, 252.milliseconds.inWholeNanoseconds)
        assertEquals(Long.MAX_VALUE, (365.days * 293).inWholeNanoseconds) // clamping overflowed value

        repeat(100) {
            val value = Random.nextLong(1000)
            val unit = units.random()
            val unit2 = units.random()

            @OptIn(ExperimentalTime::class)
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
            val isNsRange = Random.nextBoolean()
            val d = if (isNsRange)
                Random.nextLong(365L * 146)
            else
                Random.nextLong(365L * 150, 365L * 146_000_000)
            val h = Random.nextInt(24)
            val m = Random.nextInt(60)
            val s = Random.nextInt(60)
            val ns = Random.nextInt(1e9.toInt())
            val expectedNs = if (isNsRange) ns else ns - (ns % NANOS_IN_MILLIS)
            (d.days + h.hours + m.minutes + s.seconds + ns.nanoseconds).run {
                toComponents { seconds, nanoseconds ->
                    assertEquals(d * 86400 + h * 3600 + m * 60 + s, seconds)
                    assertEquals(expectedNs, nanoseconds)
                }
                toComponents { minutes, seconds, nanoseconds ->
                    assertEquals(d * 1440 + h * 60 + m, minutes)
                    assertEquals(s, seconds)
                    assertEquals(expectedNs, nanoseconds)
                }
                toComponents { hours, minutes, seconds, nanoseconds ->
                    assertEquals(d * 24 + h, hours)
                    assertEquals(m, minutes)
                    assertEquals(s, seconds)
                    assertEquals(expectedNs, nanoseconds)
                }
                toComponents { days, hours, minutes, seconds, nanoseconds ->
                    assertEquals(d, days)
                    assertEquals(h, hours)
                    assertEquals(m, minutes)
                    assertEquals(s, seconds)
                    assertEquals(expectedNs, nanoseconds)
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
    fun componentsOfInfinity() {
        for (d in listOf(Duration.INFINITE, -Duration.INFINITE)) {
            val expected = if (d.isPositive()) Long.MAX_VALUE else Long.MIN_VALUE
            d.toComponents { seconds, nanoseconds ->
                assertEquals(expected, seconds)
                assertEquals(0, nanoseconds)
            }
            d.toComponents { minutes: Long, seconds: Int, nanoseconds: Int ->
                assertEquals(expected, minutes)
                assertEquals(0, seconds)
                assertEquals(0, nanoseconds)
            }
            d.toComponents { hours, minutes, seconds, nanoseconds ->
                assertEquals(expected, hours)
                assertEquals(0, minutes)
                assertEquals(0, seconds)
                assertEquals(0, nanoseconds)
            }
            d.toComponents { days, hours, minutes, seconds, nanoseconds ->
                assertEquals(expected, days)
                assertEquals(0, hours)
                assertEquals(0, minutes)
                assertEquals(0, seconds)
                assertEquals(0, nanoseconds)
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
            assertEquals(Duration.ZERO.toDouble(DurationUnit.SECONDS), value.toDouble(DurationUnit.SECONDS))
            assertEquals(0, Duration.ZERO.compareTo(value))
            assertEquals(0, Duration.ZERO.toDouble(DurationUnit.NANOSECONDS).compareTo(value.toDouble(DurationUnit.NANOSECONDS)))
        }
        equivalentToZero((-0.0).seconds)
        equivalentToZero((-0.0).toDuration(DurationUnit.DAYS))
        equivalentToZero(-Duration.ZERO)
        equivalentToZero((-1).seconds / Double.POSITIVE_INFINITY)
        equivalentToZero(0.seconds / -1)
        equivalentToZero((-1).seconds * 0.0)
        equivalentToZero(0.seconds * -1)
    }


    @Test
    fun addition() {
        assertEquals(1.5.hours, 1.hours + 30.minutes)
        assertEquals(0.5.days, 6.hours + 360.minutes)
        assertEquals(0.5.seconds, 200.milliseconds + 300_000.microseconds)

        for (value in listOf(Duration.ZERO, 1.nanoseconds, (500 * 365).days)) {
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
        assertEquals(10.hours, 0.5.days - 120.minutes)
        assertEquals(850.milliseconds, 1.seconds - 150.milliseconds)
        assertEquals(1150.milliseconds, 1.seconds - (-150).milliseconds)
        assertEquals(1.milliseconds, Long.MAX_VALUE.microseconds - (Long.MAX_VALUE - 1_000).microseconds)
        assertEquals((-1).milliseconds, (Long.MAX_VALUE - 1_000).microseconds - Long.MAX_VALUE.microseconds)

        run {
            val offset = 2L * NANOS_IN_MILLIS
            val value = MAX_NANOS + offset
            val base = value.nanoseconds
            val baseNs = base.inWholeMilliseconds * NANOS_IN_MILLIS
            assertEquals(baseNs, base.inWholeNanoseconds)  // base stored as millis

            val smallDeltas = listOf(1L, 2L, 1000L, NANOS_IN_MILLIS - 1L) + List(10) { Random.nextLong(NANOS_IN_MILLIS.toLong()) }
            for (smallDeltaNs in smallDeltas) {
                assertEquals(base, base - smallDeltaNs.nanoseconds, "delta: $smallDeltaNs")
            }

            val deltas = listOf(offset + 1L, offset + 1500L) +
                    List(10) { Random.nextLong(offset + 1500, offset + 10000) } +
                    List(100) { Random.nextLong(offset + 1500, MAX_NANOS) }
            for (deltaNs in deltas) {
                val delta = deltaNs.nanoseconds
                assertEquals(deltaNs, delta.inWholeNanoseconds)
                assertEquals(baseNs - deltaNs, (base - delta).inWholeNanoseconds, "base: $baseNs, delta: $deltaNs")
            }
        }

        for (value in listOf(Duration.ZERO, 1.nanoseconds, (500 * 365).days)) {
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
        assertEquals(1.days, 12.hours * 2)
        assertEquals(1.days, 60.minutes * 24.0)
        assertEquals(1.microseconds, 20.nanoseconds * 50)

        assertEquals(1.days, 2 * 12.hours)
        assertEquals(12.5.hours, 12.5 * 60.minutes)
        assertEquals(1.microseconds, 50 * 20.nanoseconds)

        assertEquals(Duration.ZERO, 0 * 1.hours)
        assertEquals(Duration.ZERO, 1.seconds * 0.0)

        run { // promoting nanos range to millis range after multiplication
            val value = MAX_NANOS
            assertEquals(value, (value.nanoseconds * 1_000_000).inWholeMilliseconds)
            assertEquals(value / 1000, (value.nanoseconds * 1_000).inWholeMilliseconds)
            assertEquals(Duration.INFINITE, (Long.MAX_VALUE / 1000 + 1).nanoseconds * 1_000_000_000)
        }

        run {
            val value = MAX_NANOS / Int.MAX_VALUE
            assertTrue((value.nanoseconds * Int.MIN_VALUE).inWholeNanoseconds < -MAX_NANOS)
        }

        assertEquals(Duration.INFINITE, Int.MAX_VALUE.days * Int.MAX_VALUE)
        assertEquals(-Duration.INFINITE, Int.MAX_VALUE.days * Int.MIN_VALUE)

        assertEquals(Duration.INFINITE, Duration.INFINITE * Double.POSITIVE_INFINITY)
        assertEquals(Duration.INFINITE, Duration.INFINITE * Double.MIN_VALUE)
        assertEquals(-Duration.INFINITE, Duration.INFINITE * Double.NEGATIVE_INFINITY)
        assertEquals(-Duration.INFINITE, Duration.INFINITE * -1)

        assertFailsWith<IllegalArgumentException> { Duration.INFINITE * 0 }
        assertFailsWith<IllegalArgumentException> { 0.0 * Duration.INFINITE }
    }

    @Test
    fun divisionByNumber() {
        assertEquals(12.hours, 1.days / 2)
        assertEquals(60.minutes, 1.days / 24.0)
        assertEquals(20.seconds, 2.minutes / 6)
        assertEquals(365.days, (365 * 299).days / 299)
        assertEquals(365.days, (365 * 299.5).days / 299.5)

        run {
            val value = MAX_NANOS
            assertEquals(value, (value.milliseconds / 1_000_000).inWholeNanoseconds)
        }

        assertEquals(Duration.INFINITE, 1.seconds / 0)
        assertEquals(-Duration.INFINITE, -1.seconds / 0.0)
        assertEquals(Duration.INFINITE, -1.seconds / (-0.0))

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
        assertEquals(24.0, 1.days / 1.hours)
        assertEquals(0.1, 9.minutes / 1.5.hours)
        assertEquals(50.0, 1.microseconds / 20.nanoseconds)
        assertEquals(299.0, (365 * 299).days / 365.days)

        assertTrue((Duration.INFINITE / Duration.INFINITE).isNaN())
        assertTrue((Duration.ZERO / Duration.ZERO).isNaN())
    }

    @Test
    fun truncation() {
        fun expect(expected: Duration, value: Duration, unit: DurationUnit) {
            assertEquals(expected, value.truncateTo(unit))
            assertEquals(-expected, (-value).truncateTo(unit))
        }
        for (unit in units) {
            expect(Duration.ZERO, Duration.ZERO, unit)
            expect(Duration.INFINITE, Duration.INFINITE, unit)
            expect(Duration.ZERO, 1.toDuration(unit) - 1.nanoseconds, unit)
            repeat(100) {
                val whole = Random.nextInt(100_000).toDuration(unit)
                expect(whole, whole, unit)
                if (unit > DurationUnit.NANOSECONDS) {
                    val part = Random.nextLong(1, 1.toDuration(unit).inWholeNanoseconds).nanoseconds
                    expect(Duration.ZERO, part, unit)
                    expect(whole, whole + part, unit)
                }
            }
        }
        repeat(10) {
            val d = Random.nextLong().nanoseconds
            expect(d, d, DurationUnit.NANOSECONDS)
        }
        expect(12.microseconds, 12998.nanoseconds, DurationUnit.MICROSECONDS)
        expect(1503.milliseconds, 1503_889_404.nanoseconds, DurationUnit.MILLISECONDS)
        expect(340.seconds, 340_990_567_444L.nanoseconds, DurationUnit.SECONDS)
        expect(3.minutes, 200.seconds, DurationUnit.MINUTES)
        expect(4.hours, 250.minutes, DurationUnit.HOURS)
        expect(1.days, 30.hours, DurationUnit.DAYS)

        // big durations
        run {
            val d = (Long.MAX_VALUE / 4).milliseconds
            for (unit in units) {
                if (unit <= DurationUnit.MILLISECONDS) {
                    expect(d, d, unit)
                } else {
                    expect(d.toLong(unit).toDuration(unit), d, unit)
                }
            }
        }
    }

    @Test
    fun parseAndFormatIsoString() {
        fun test(duration: Duration, vararg isoStrings: String) {
            assertEquals(isoStrings.first(), duration.toIsoString())
            for (isoString in isoStrings) {
                assertEquals(duration, Duration.parseIsoString(isoString), isoString)
                assertEquals(duration, Duration.parse(isoString), isoString)
                assertEquals(duration, Duration.parseIsoStringOrNull(isoString), isoString)
                assertEquals(duration, Duration.parseOrNull(isoString), isoString)
            }
        }

        // zero
        test(Duration.ZERO, "PT0S", "P0D", "PT0H", "PT0M", "P0DT0H", "PT0H0M", "PT0H0S")

        // single unit
        test(1.days, "PT24H", "P1D", "PT1440M", "PT86400S")
        test(1.hours, "PT1H")
        test(1.minutes, "PT1M")
        test(1.seconds, "PT1S")
        test(1.milliseconds, "PT0.001S")
        test(1.microseconds, "PT0.000001S")
        test(1.nanoseconds, "PT0.000000001S", "PT0.0000000009S")
        test(0.9.nanoseconds, "PT0.000000001S")

        // rounded to zero
        test(0.1.nanoseconds, "PT0S")
        test(Duration.ZERO, "PT0S", "PT0.0000000004S")

        // several units combined
        test(1.days + 1.minutes, "PT24H1M")
        test(1.days + 1.seconds, "PT24H0M1S")
        test(1.days + 1.milliseconds, "PT24H0M0.001S")
        test(1.hours + 30.minutes, "PT1H30M")
        test(1.hours + 500.milliseconds, "PT1H0M0.500S")
        test(2.minutes + 500.milliseconds, "PT2M0.500S")
        test(90_500.milliseconds, "PT1M30.500S")

        // with sign
        test(-1.days + 15.minutes, "-PT23H45M", "PT-23H-45M", "+PT-24H+15M")
        test(-1.days - 15.minutes, "-PT24H15M", "PT-24H-15M", "-PT25H-45M")
        test(Duration.ZERO, "PT0S", "P1DT-24H", "+PT-1H+60M", "-PT1M-60S")

        // infinite
        test(Duration.INFINITE, "PT9999999999999H", "PT+10000000000000H", "-PT-9999999999999H", "-PT-1234567890123456789012S")
        test(-Duration.INFINITE, "-PT9999999999999H", "-PT10000000000000H", "PT-1234567890123456789012S")
    }

    @Test
    fun parseIsoStringFailing() {
        for (invalidValue in listOf(
            "", " ", "P", "PT", "P1DT", "P1", "PT1", "0", "+P", "+", "-", "h", "H", "something",
            "1m", "1d", "2d 11s", "Infinity", "-Infinity",
            "P+12+34D", "P12-34D", "PT1234567890-1234567890S",
            " P1D", "PT1S ",
            "P3W",
            "P1Y", "P1M", "P1S", "PT1D", "PT1Y",
            "PT1S2S", "PT1S2H",
            "P9999999999999DT-9999999999999H",
            "PT1.5H", "PT0.5D", "PT.5S", "PT0.25.25S",
        )) {
            assertNull(Duration.parseIsoStringOrNull(invalidValue), invalidValue)
            assertFailsWith<IllegalArgumentException>(invalidValue) { Duration.parseIsoString(invalidValue) }.let { e ->
                assertContains(e.message!!, "'$invalidValue'")
            }
        }

    }

    @Test
    fun parseAndFormatInUnits() {
        if (TestPlatform.current == TestPlatform.WasmWasi) return

        var d = 1.days + 15.hours + 31.minutes + 45.seconds +
                678.milliseconds + 920.microseconds + 516.34.nanoseconds

        fun test(unit: DurationUnit, vararg representations: String) {
            assertFails { d.toString(unit, -1) }
            assertEquals(representations.toList(), representations.indices.map { d.toString(unit, it) })
            for ((decimals, string) in representations.withIndex()) {
                val d1 = Duration.parse(string)
                assertEquals(d1, Duration.parseOrNull(string))
                if (!(d1 == d || (d1 - d).absoluteValue <= (0.5 * 10.0.pow(-decimals)).toDuration(unit))) {
                    fail("Parsed value $d1 (from $string) is too far from the real value $d")
                }
            }
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
        // no sub-nanosecond precision
        test(DurationUnit.NANOSECONDS, "516ns", "516.0ns", "516.00ns", "516.000ns", "516.0000ns")
        d = (d - 516.nanoseconds) / 17
        test(DurationUnit.NANOSECONDS, "0ns", "0.0ns", "0.00ns", "0.000ns", "0.0000ns")

        // infinite
//        d = Duration.nanoseconds(Double.MAX_VALUE)
//        test(DurationUnit.DAYS, "2.08e+294d")
//        test(DurationUnit.NANOSECONDS, "1.80e+308ns")

        assertEquals("0.500000000000s", 0.5.seconds.toString(DurationUnit.SECONDS, 100))
        assertEquals("99999000000000.000000000000ns", 99_999.seconds.toString(DurationUnit.NANOSECONDS, 15))
        assertContains(
            listOf(
                "-4611686018427388000000000.000000000000ns",
                "-4611686018427387904000000.000000000000ns"
            ),
            (-(MAX_MILLIS - 1).milliseconds).toString(DurationUnit.NANOSECONDS, 15)
        )

        d = Duration.INFINITE
        test(DurationUnit.DAYS, "Infinity", "Infinity")
        d = -Duration.INFINITE
        test(DurationUnit.NANOSECONDS, "-Infinity", "-Infinity")
    }


    @Test
    fun parseAndFormatDefault() {
        fun testParsing(string: String, expectedDuration: Duration) {
            assertEquals(expectedDuration, Duration.parse(string), string)
            assertEquals(expectedDuration, Duration.parseOrNull(string), string)
        }

        fun test(duration: Duration, vararg expected: String) {
            val actual = duration.toString()
            assertEquals(expected.first(), actual)

            if (duration.isPositive()) {
                if (' ' in actual) {
                    assertEquals("-($actual)", (-duration).toString())
                } else {
                    assertEquals("-$actual", (-duration).toString())
                }
            }

            for (string in expected) {
                testParsing(string, duration)
                if (duration.isPositive() && duration.isFinite()) {
                    testParsing("+($string)", duration)
                    testParsing("-($string)", -duration)
                    if (' ' !in string) {
                        testParsing("+$string", duration)
                        testParsing("-$string", -duration)
                    }
                }
            }
        }

        test(101.days, "101d", "2424h")
        test(45.3.days, "45d 7h 12m", "45.3d", "45d 7.2h") // 0.3d == 7.2h
        test(45.days, "45d")

        test(40.5.days, "40d 12h", "40.5d", "40d 720m")
        test(40.days + 20.minutes, "40d 0h 20m", "40d 20m", "40d 1200s")
        test(40.days + 20.seconds, "40d 0h 0m 20s", "40d 20s")
        test(40.days + 100.nanoseconds, "40d 0h 0m 0.000000100s", "40d 100ns")

        test(40.hours + 15.minutes, "1d 16h 15m", "40h 15m")
        test(40.hours, "1d 16h", "40h")

        test(12.5.hours, "12h 30m")
        test(12.hours + 15.seconds, "12h 0m 15s")
        test(12.hours + 1.nanoseconds, "12h 0m 0.000000001s")
        test(30.minutes, "30m")
        test(17.5.minutes, "17m 30s")

        test(16.5.minutes, "16m 30s")
        test(1097.1.seconds, "18m 17.1s")
        test(90.36.seconds, "1m 30.36s")
        test(50.seconds, "50s")
        test(1.3.seconds, "1.3s")
        test(1.seconds, "1s")

        test(0.5.seconds, "500ms")
        test(40.2.milliseconds, "40.2ms")
        test(4.225.milliseconds, "4.225ms")
        test(4.24501.milliseconds, "4.245010ms", "4ms 245us 10ns")
        test(1.milliseconds, "1ms")

        test(0.75.milliseconds, "750us")
        test(75.35.microseconds, "75.35us")
        test(7.25.microseconds, "7.25us")
        test(1.035.microseconds, "1.035us")
        test(1.005.microseconds, "1.005us")
        test(1800.nanoseconds, "1.8us", "1800ns", "0.0000000005h")

        test(950.5.nanoseconds, "951ns")
        test(85.23.nanoseconds, "85ns")
        test(8.235.nanoseconds, "8ns")
        test(1.nanoseconds, "1ns", "0.9ns", "0.001us", "0.0009us")
        test(1.3.nanoseconds, "1ns")
        test(0.75.nanoseconds, "1ns")
        test(0.7512.nanoseconds, "1ns")

        // equal to zero
//        test(0.023.nanoseconds, "0.023ns")
//        test(0.0034.nanoseconds, "0.0034ns")
//        test(0.0000035.nanoseconds, "0.0000035ns")

        test(Duration.ZERO, "0s", "0.4ns", "0000.0000ns")
        test(365.days * 10000, "3650000d")
        test(300.days * 100000, "30000000d")
        test(365.days * 100000, "36500000d")
        test((MAX_MILLIS - 1).milliseconds, "53375995583d 15h 36m 27.902s") // max finite value

        // all infinite
//        val universeAge = Duration.days(365.25) * 13.799e9
//        val planckTime = Duration.seconds(5.4e-44)

//        test(universeAge, "5.04e+12d")
//        test(planckTime, "5.40e-44s")
//        test(Duration.nanoseconds(Double.MAX_VALUE), "2.08e+294d")
        test(Duration.INFINITE, "Infinity", "53375995583d 20h", "+Infinity")
        test(-Duration.INFINITE, "-Infinity", "-(53375995583d 20h)")
    }

    @Test
    fun parseDefaultFailing() {
        for (invalidValue in listOf(
            "", " ", "P", "PT", "P1DT", "P1", "PT1", "0", "+P", "+", "-", "h", "H", "something",
            "1234567890123456789012ns", "Inf", "-Infinity value",
            "1s ", " 1s",
            "1d 1m 1h", "1s 2s",
            "-12m 15s", "-12m -15s", "-()", "-(12m 30s",
            "+12m 15s", "+12m +15s", "+()", "+(12m 30s",
            "()", "(12m 30s)",
            "12.5m 11.5s", ".2s", "0.1553.39m",
            "P+12+34D", "P12-34D", "PT1234567890-1234567890S",
            " P1D", "PT1S ",
            "P1Y", "P1M", "P1S", "PT1D", "PT1Y",
            "PT1S2S", "PT1S2H",
            "P9999999999999DT-9999999999999H",
            "PT1.5H", "PT0.5D", "PT.5S", "PT0.25.25S",
        )) {
            assertNull(Duration.parseOrNull(invalidValue), invalidValue)
            assertFailsWith<IllegalArgumentException>(invalidValue) { Duration.parse(invalidValue) }.let { e ->
                assertContains(e.message!!, "'$invalidValue'")
            }
        }
    }

}
