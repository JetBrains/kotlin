/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package test.time

import kotlin.random.*
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class InstantTest {
    private val largePositiveLongs = listOf(Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE - 50)
    private val largeNegativeLongs = listOf(Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 50)

    private val largePositiveInstants = listOf(Instant.MAX, Instant.MAX - 1.seconds, Instant.MAX - 50.seconds)
    private val largeNegativeInstants = listOf(Instant.MIN, Instant.MIN + 1.seconds, Instant.MIN + 50.seconds)

    private val smallInstants = listOf(
        Instant.fromEpochMilliseconds(0),
        Instant.fromEpochMilliseconds(1003),
        Instant.fromEpochMilliseconds(253112)
    )

    @Test
    fun systemClockNow() {
        val instant = Clock.System.now()
        val millis = instant.toEpochMilliseconds()

        assertTrue(millis > 1_500_000_000_000L, "instant: $instant, millis: $millis")

        val millisInstant = Instant.fromEpochMilliseconds(millis)

        assertEquals(millis, millisInstant.toEpochMilliseconds())

        val notEqualInstant = Instant.fromEpochMilliseconds(millis + 1)
        assertNotEquals(notEqualInstant, instant)
    }

    @Test
    fun instantArithmetic() {
        val instant = Clock.System.now().toEpochMilliseconds().let { Instant.fromEpochMilliseconds(it) } // round to millis
        val diffMillis = Random.nextLong(1000, 1_000_000_000)
        val diff = diffMillis.milliseconds

        val nextInstant = (instant.toEpochMilliseconds() + diffMillis).let { Instant.fromEpochMilliseconds(it) }

        assertEquals(diff, nextInstant - instant)
        assertEquals(nextInstant, instant + diff)
        assertEquals(instant, nextInstant - diff)
    }

    @Test
    fun addingMultiplesOf2_32() {
        val pow2_32 = 1L shl 32
        val instant1 = Instant.fromEpochSeconds(0)
        val instant2 = instant1.plus(pow2_32.toDuration(DurationUnit.NANOSECONDS))
        assertEquals(pow2_32 / NANOS_PER_SECOND, instant2.epochSeconds)
        assertEquals(pow2_32 % NANOS_PER_SECOND, instant2.nanosecondsOfSecond.toLong())

        val instant3 = instant1.plus(pow2_32.toDuration(DurationUnit.SECONDS))
        assertEquals(pow2_32, instant3.epochSeconds)
    }

    @Test
    fun nanosecondAdjustment() {
        for (i in -2..2L) {
            for (j in 0..9) {
                val t: Instant = Instant.fromEpochSeconds(i, j)
                val t2: Instant = Instant.fromEpochSeconds(i, j.toLong())
                assertEquals(i, t.epochSeconds)
                assertEquals(j, t.nanosecondsOfSecond)
                assertEquals(t, t2)
            }
            for (j in -10..-1) {
                val t: Instant = Instant.fromEpochSeconds(i, j)
                val t2: Instant = Instant.fromEpochSeconds(i, j.toLong())
                assertEquals(i - 1, t.epochSeconds)
                assertEquals(j + 1000000000, t.nanosecondsOfSecond)
                assertEquals(t, t2)
            }
            for (j in 999_999_990..999_999_999) {
                val t: Instant = Instant.fromEpochSeconds(i, j)
                val t2: Instant = Instant.fromEpochSeconds(i, j.toLong())
                assertEquals(i, t.epochSeconds)
                assertEquals(j, t.nanosecondsOfSecond)
                assertEquals(t, t2)
            }
        }
        val t = Instant.fromEpochSeconds(0, Int.MAX_VALUE)
        assertEquals((Int.MAX_VALUE / 1_000_000_000).toLong(), t.epochSeconds)
        assertEquals(Int.MAX_VALUE % 1_000_000_000, t.nanosecondsOfSecond)
        val t2 = Instant.fromEpochSeconds(0, Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE / 1_000_000_000, t2.epochSeconds)
        assertEquals((Long.MAX_VALUE % 1_000_000_000).toInt(), t2.nanosecondsOfSecond)
    }

    @Test
    fun distantPastAndFuture() {
        val distantFutureString = "+100000-01-01T00:00:00Z"
        val distantPastString = "-100001-12-31T23:59:59.999999999Z"
        assertEquals(distantFutureString, Instant.DISTANT_FUTURE.toString())
        assertEquals(Instant.DISTANT_FUTURE, Instant.parse(distantFutureString))
        assertEquals(distantPastString, Instant.DISTANT_PAST.toString())
        assertEquals(Instant.DISTANT_PAST, Instant.parse(distantPastString))
        assertTrue(Instant.DISTANT_PAST.isDistantPast)
        assertTrue(Instant.DISTANT_FUTURE.isDistantFuture)
        assertFalse(Instant.DISTANT_PAST.isDistantFuture)
        assertFalse(Instant.DISTANT_FUTURE.isDistantPast)
        assertFalse((Instant.DISTANT_PAST + 1.nanoseconds).isDistantPast)
        assertFalse((Instant.DISTANT_FUTURE - 1.nanoseconds).isDistantFuture)
        assertTrue((Instant.DISTANT_PAST - 1.nanoseconds).isDistantPast)
        assertTrue((Instant.DISTANT_FUTURE + 1.nanoseconds).isDistantFuture)
        assertTrue(Instant.MAX.isDistantFuture)
        assertFalse(Instant.MAX.isDistantPast)
        assertTrue(Instant.MIN.isDistantPast)
        assertFalse(Instant.MIN.isDistantFuture)
    }

    @Test
    fun epochMillisecondsClamping() {
        /* Any number of milliseconds in Long is representable as an Instant */
        for (instant in largePositiveInstants) {
            assertEquals(Long.MAX_VALUE, instant.toEpochMilliseconds(), "$instant")
        }
        for (instant in largeNegativeInstants) {
            assertEquals(Long.MIN_VALUE, instant.toEpochMilliseconds(), "$instant")
        }
        for (milliseconds in largePositiveLongs + largeNegativeLongs) {
            assertEquals(milliseconds, Instant.fromEpochMilliseconds(milliseconds).toEpochMilliseconds(),
                    "$milliseconds")
        }
    }

    @Test
    fun epochSecondsClamping() {
        // fromEpochSeconds
        // On all platforms Long.MAX_VALUE of seconds is not a valid instant.
        for (seconds in largePositiveLongs) {
            assertEquals(Instant.MAX, Instant.fromEpochSeconds(seconds, 35))
        }
        for (seconds in largeNegativeLongs) {
            assertEquals(Instant.MIN, Instant.fromEpochSeconds(seconds, 35))
        }
        for (instant in largePositiveInstants + smallInstants + largeNegativeInstants) {
            assertEquals(instant, Instant.fromEpochSeconds(instant.epochSeconds, instant.nanosecondsOfSecond.toLong()))
        }
    }

    @Test
    fun durationArithmeticClamping() {
        val longDurations = listOf(Duration.INFINITE)

        for (duration in longDurations) {
            for (instant in smallInstants + largeNegativeInstants + largePositiveInstants) {
                assertEquals(Instant.MAX, instant + duration)
            }
            for (instant in smallInstants + largeNegativeInstants + largePositiveInstants) {
                assertEquals(Instant.MIN, instant - duration)
            }
        }
        assertEquals(Instant.MAX, (Instant.MAX - 4.seconds) + 5.seconds)
        assertEquals(Instant.MIN, (Instant.MIN + 10.seconds) - 12.seconds)
    }

    @Test
    fun arithmeticOutOfRange() {
        // Arithmetic overflow
        for (instant in smallInstants + largeNegativeInstants + largePositiveInstants) {
            assertEquals(Instant.MAX, instant.plus(Long.MAX_VALUE.seconds))
            assertEquals(Instant.MIN, instant.plus(Long.MIN_VALUE.seconds))
        }
        // Overflow of Instant boundaries
        for (instant in smallInstants + largeNegativeInstants + largePositiveInstants) {
            assertEquals(Instant.MAX, instant.plus((Instant.MAX.epochSeconds - instant.epochSeconds + 1).seconds))
            assertEquals(Instant.MIN, instant.plus((Instant.MIN.epochSeconds - instant.epochSeconds - 1).seconds))
        }
    }

    // https://github.com/Kotlin/kotlinx-datetime/issues/263
    @Test
    fun addSmallDurationsToLargeInstants() {
        for (smallDuration in listOf(1.nanoseconds, 999_999.nanoseconds, 1.seconds - 1.nanoseconds)) {
            assertEquals(expected = Instant.MAX, actual = Instant.MAX + smallDuration)
            assertEquals(expected = Instant.MIN, actual = Instant.MIN - smallDuration)
        }
    }

    @Test
    fun subtractInstants() {
        val max = Instant.fromEpochSeconds(31494816403199L)
        val min = Instant.fromEpochSeconds(-31619119219200L)
        assertEquals(max.epochSeconds - min.epochSeconds, (max - min).inWholeSeconds)
    }

    @Test
    fun compareInstants() {
        val instants = arrayOf(
            Instant.fromEpochSeconds(-2L, 0),
            Instant.fromEpochSeconds(-2L, 999999998),
            Instant.fromEpochSeconds(-2L, 999999999),
            Instant.fromEpochSeconds(-1L, 0),
            Instant.fromEpochSeconds(-1L, 1),
            Instant.fromEpochSeconds(-1L, 999999998),
            Instant.fromEpochSeconds(-1L, 999999999),
            Instant.fromEpochSeconds(0L, 0),
            Instant.fromEpochSeconds(0L, 1),
            Instant.fromEpochSeconds(0L, 2),
            Instant.fromEpochSeconds(0L, 999999999),
            Instant.fromEpochSeconds(1L, 0),
            Instant.fromEpochSeconds(2L, 0)
        )
        for (i in instants.indices) {
            val a = instants[i]
            for (j in instants.indices) {
                val b = instants[j]
                when {
                    i < j -> {
                        assertTrue(a < b, "$a <=> $b")
                        assertNotEquals(a, b, "$a <=> $b")
                    }
                    i > j -> {
                        assertTrue(a > b, "$a <=> $b")
                        assertNotEquals(a, b, "$a <=> $b")
                    }
                    else -> {
                        assertEquals(0, a.compareTo(b), "$a <=> $b")
                        assertEquals(a, b, "$a <=> $b")
                    }
                }
            }
        }
    }

    @Test
    fun instantEquals() {
        val test5a: Instant = Instant.fromEpochSeconds(5L, 20)
        val test5b: Instant = Instant.fromEpochSeconds(5L, 20)
        val test5n: Instant = Instant.fromEpochSeconds(5L, 30)
        val test6: Instant = Instant.fromEpochSeconds(6L, 20)

        assertEquals(test5a, test5a)
        assertEquals(test5a, test5b)
        assertNotEquals(test5a, test5n)
        assertNotEquals(test5a, test6)
        assertEquals(test5b, test5a)
        assertEquals(test5b, test5b)
        assertNotEquals(test5b, test5n)
        assertNotEquals(test5b, test6)
        assertNotEquals(test5n, test5a)
        assertNotEquals(test5n, test5b)
        assertEquals(test5n, test5n)
        assertNotEquals(test5n, test6)
        assertNotEquals(test6, test5a)
        assertNotEquals(test6, test5b)
        assertNotEquals(test6, test5n)
        assertEquals(test6, test6)
    }

    @Test
    fun toEpochMilliseconds() {
        assertEquals(Instant.fromEpochSeconds(1L, 1000000).toEpochMilliseconds(), 1001L)
        assertEquals(Instant.fromEpochSeconds(1L, 2000000).toEpochMilliseconds(), 1002L)
        assertEquals(Instant.fromEpochSeconds(1L, 567).toEpochMilliseconds(), 1000L)
        assertEquals(Instant.fromEpochSeconds(Long.MAX_VALUE / 1_000_000).toEpochMilliseconds(), Long.MAX_VALUE / 1_000_000 * 1000)
        assertEquals(Instant.fromEpochSeconds(Long.MIN_VALUE / 1_000_000).toEpochMilliseconds(), Long.MIN_VALUE / 1_000_000 * 1000)
        assertEquals(Instant.fromEpochSeconds(0L, -1000000).toEpochMilliseconds(), -1L)
        assertEquals(Instant.fromEpochSeconds(0L, 1000000).toEpochMilliseconds(), 1)
        assertEquals(Instant.fromEpochSeconds(0L, 999999).toEpochMilliseconds(), 0)
        assertEquals(Instant.fromEpochSeconds(0L, 1).toEpochMilliseconds(), 0)
        assertEquals(Instant.fromEpochSeconds(0L, 0).toEpochMilliseconds(), 0)
        assertEquals(Instant.fromEpochSeconds(0L, -1).toEpochMilliseconds(), -1L)
        assertEquals(Instant.fromEpochSeconds(0L, -999999).toEpochMilliseconds(), -1L)
        assertEquals(Instant.fromEpochSeconds(0L, -1000000).toEpochMilliseconds(), -1L)
        assertEquals(Instant.fromEpochSeconds(0L, -1000001).toEpochMilliseconds(), -2L)
    }

    @Test
    fun isLeapYear() {
        assertEquals(false, isLeapYear(1999))
        assertEquals(true, isLeapYear(2000))
        assertEquals(false, isLeapYear(2001))
        assertEquals(false, isLeapYear(2007))
        assertEquals(true, isLeapYear(2008))
        assertEquals(false, isLeapYear(2009))
        assertEquals(false, isLeapYear(2010))
        assertEquals(false, isLeapYear(2011))
        assertEquals(true, isLeapYear(2012))
        assertEquals(false, isLeapYear(2095))
        assertEquals(true, isLeapYear(2096))
        assertEquals(false, isLeapYear(2097))
        assertEquals(false, isLeapYear(2098))
        assertEquals(false, isLeapYear(2099))
        assertEquals(false, isLeapYear(2100))
        assertEquals(false, isLeapYear(2101))
        assertEquals(false, isLeapYear(2102))
        assertEquals(false, isLeapYear(2103))
        assertEquals(true, isLeapYear(2104))
        assertEquals(false, isLeapYear(2105))
        assertEquals(false, isLeapYear(-500))
        assertEquals(true, isLeapYear(-400))
        assertEquals(false, isLeapYear(-300))
        assertEquals(false, isLeapYear(-200))
        assertEquals(false, isLeapYear(-100))
        assertEquals(true, isLeapYear(0))
        assertEquals(false, isLeapYear(100))
        assertEquals(false, isLeapYear(200))
        assertEquals(false, isLeapYear(300))
        assertEquals(true, isLeapYear(400))
        assertEquals(false, isLeapYear(500))
    }
}
