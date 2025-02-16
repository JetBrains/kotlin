/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import samples.*
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds

class Instants {

    @Sample
    fun epochSecondsAndNanosecondsOfSecond() {
        val currentInstant = Clock.System.now()
        // The number of whole seconds that have passed since the Unix epoch.
        println(currentInstant.epochSeconds)
        // The number of nanoseconds that passed since the start of the second.
        println(currentInstant.nanosecondsOfSecond)
    }

    @Sample
    fun fromEpochSecondsProperties() {
        // When nanosecondAdjustment is within the range `0..999_999_999`,
        // epochSeconds and nanosecondsOfSecond hold the exact values provided.
        val instant1 = Instant.fromEpochSeconds(999_999, nanosecondAdjustment = 123_456_789)
        assertPrints(instant1.epochSeconds, "999999")
        assertPrints(instant1.nanosecondsOfSecond, "123456789")

        // When nanosecondAdjustment exceeds `999_999_999`, the excess contributes to whole seconds,
        // increasing epochSeconds. The remainder forms the new value of nanosecondsOfSecond.
        val instant2 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = 100_123_456_789)
        assertPrints(instant2.epochSeconds, "1000100")
        assertPrints(instant2.nanosecondsOfSecond, "123456789")

        // When nanosecondAdjustment is negative, epochSeconds is decreased
        // to offset the negative nanoseconds and ensure nanosecondsOfSecond remains non-negative.
        val instant3 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = -100_876_543_211)
        assertPrints(instant3.epochSeconds, "999899")
        assertPrints(instant3.nanosecondsOfSecond, "123456789")
    }

    @Sample
    fun toEpochMilliseconds() {
        val currentInstant = Clock.System.now()
        // The number of whole milliseconds that have passed since the Unix epoch.
        println(currentInstant.toEpochMilliseconds())

        // When an Instant is created with fromEpochMilliseconds,
        // toEpochMilliseconds will return the exact value provided.
        val instant1 = Instant.fromEpochMilliseconds(0)
        assertPrints(instant1.toEpochMilliseconds(), "0")
        val instant2 = Instant.fromEpochMilliseconds(1_000_000_000_123)
        assertPrints(instant2.toEpochMilliseconds(), "1000000000123")

        // Any fractional part of a millisecond is rounded toward zero to the whole number of milliseconds.
        val instant3 = Instant.fromEpochSeconds(1_000_000_000, nanosecondAdjustment = 123_999_999)
        assertPrints(instant3.toEpochMilliseconds(), "1000000000123")
    }

    @Sample
    fun plusDuration() {
        // Finding a moment that's later than the starting point by the given amount of real time
        val instant = Instant.fromEpochSeconds(7 * 60 * 60, nanosecondAdjustment = 123_456_789)
        val fiveHoursLater = instant + 5.hours
        assertPrints(fiveHoursLater.epochSeconds == 12 * 60 * 60L, "true")
        assertPrints(fiveHoursLater.nanosecondsOfSecond == 123_456_789, "true")
    }

    @Sample
    fun minusDuration() {
        // Finding a moment that's earlier than the starting point by the given amount of real time
        val instant = Instant.fromEpochSeconds(7 * 60 * 60, nanosecondAdjustment = 123_456_789)
        val fiveHoursEarlier = instant - 5.hours
        assertPrints(fiveHoursEarlier.epochSeconds == 2 * 60 * 60L, "true")
        assertPrints(fiveHoursEarlier.nanosecondsOfSecond == 123_456_789, "true")
    }

    @Sample
    fun minusInstant() {
        // Finding the difference between two instants in terms of elapsed time
        val instant1 = Instant.fromEpochSeconds(0) // The Unix epoch start.
        val instant2 = Instant.fromEpochSeconds(7 * 60 * 60) // 7 hours since the Unix epoch.
        val elapsedTime = instant1 - instant2 // The duration between instant1 and instant2.
        assertPrints(elapsedTime == (-7).hours, "true")
    }

    @Sample
    fun compareToSample() {
        val instant1 = Instant.parse("1970-01-01T00:00:00Z") // The Unix epoch.
        val instant2 = Instant.parse("2025-01-06T13:36:44Z")
        val instant3 = Instant.fromEpochSeconds(0) // The Unix epoch.

        assertTrue(instant1 < instant2)
        assertFalse(instant3 > instant1)
        assertTrue(instant3 == instant1)
    }

    @Sample
    fun toStringSample() {
        // The string representation of the current instant in ISO 8601 format.
        val currentInstant = Clock.System.now()
        println(currentInstant.toString())
        // The string representation of the Unix epoch.
        val unixEpochInstant = Instant.fromEpochSeconds(0)
        assertPrints(unixEpochInstant.toString(), "1970-01-01T00:00:00Z")
        // The string representation of an instant.
        val instant = Instant.fromEpochMilliseconds(1_000_000_000_123)
        assertPrints(instant.toString(), "2001-09-09T01:46:40.123Z")
    }

    @Sample
    fun fromEpochMilliseconds() {
        // Creating an Instant representing the Unix epoch.
        val instant1 = Instant.fromEpochMilliseconds(0)
        // Creating an Instant from a specific number of milliseconds since the Unix epoch.
        val instant2 = Instant.fromEpochMilliseconds(1_000_000_000_123)

        // toEpochMilliseconds returns the exact millisecond value provided during construction.
        assertPrints(instant1.toEpochMilliseconds(), "0")
        assertPrints(instant2.toEpochMilliseconds(), "1000000000123")

        // The string representation of the constructed instants.
        assertPrints(instant1, "1970-01-01T00:00:00Z")
        assertPrints(instant2, "2001-09-09T01:46:40.123Z")
    }

    @Sample
    fun fromEpochSeconds() {
        // Creating an Instant representing the Unix epoch.
        val unixEpochInstant = Instant.fromEpochSeconds(0)
        // Creating an Instant from a specific number of milliseconds and nanoseconds since the Unix epoch.
        val instant = Instant.fromEpochSeconds(999_999_999, nanosecondAdjustment = 999_999_999)

        // The string representation of the constructed instants.
        assertPrints(unixEpochInstant, "1970-01-01T00:00:00Z")
        assertPrints(instant, "2001-09-09T01:46:39.999999999Z")
    }

    @Sample
    fun parsing() {
        // Parsing a string that represents the Unix epoch.
        val unixEpochInstant = Instant.parse("1970-01-01T00:00:00Z")
        assertPrints(unixEpochInstant.epochSeconds, "0")

        // Parsing an ISO 8601 string with a time zone offset.
        val instant = Instant.parse("2020-08-30T18:40:00+03:00")
        // Instants are presented in the UTC time zone when converted to string.
        assertPrints(instant.toString(), "2020-08-30T15:40:00Z")
    }

    @Sample
    fun isDistantPast() {
        // Checking if an instant is so far in the past that it's probably irrelevant.
        val tenThousandYearsBC = Instant.parse("-10000-01-01T00:00:00Z")
        assertPrints(tenThousandYearsBC.isDistantPast, "false")

        // Instant.DISTANT_PAST is the latest instant that is considered far in the past.
        assertPrints(Instant.DISTANT_PAST, "-100001-12-31T23:59:59.999999999Z")
        assertPrints(Instant.DISTANT_PAST.isDistantPast, "true")

        // All instants earlier than Instant.DISTANT_PAST are considered to be far in the past.
        val earlierThanDistantPast = Instant.DISTANT_PAST - 1.nanoseconds
        assertPrints(earlierThanDistantPast, "-100001-12-31T23:59:59.999999998Z")
        assertPrints(earlierThanDistantPast.isDistantPast, "true")

        // All instants later than Instant.DISTANT_PAST are not considered to be far in the past.
        val laterThanDistantPast = Instant.DISTANT_PAST + 1.nanoseconds
        assertPrints(laterThanDistantPast, "-100000-01-01T00:00:00Z")
        assertPrints(laterThanDistantPast.isDistantPast, "false")
    }

    @Sample
    fun isDistantFuture() {
        // Checking if an instant is so far in the future that it's probably irrelevant.
        val yearTenThousand = Instant.parse("+10000-01-01T00:00:00Z")
        assertPrints(yearTenThousand.isDistantFuture, "false")

        // Instant.DISTANT_FUTURE is the earliest instant that is considered far in the future.
        assertPrints(Instant.DISTANT_FUTURE, "+100000-01-01T00:00:00Z")
        assertPrints(Instant.DISTANT_FUTURE.isDistantFuture, "true")

        // All instants later than Instant.DISTANT_FUTURE are considered to be far in the future.
        val laterThanDistantFuture = Instant.DISTANT_FUTURE + 1.nanoseconds
        assertPrints(laterThanDistantFuture, "+100000-01-01T00:00:00.000000001Z")
        assertPrints(laterThanDistantFuture.isDistantFuture, "true")

        // All instants earlier than Instant.DISTANT_FUTURE are not considered to be far in the future.
        val earlierThanDistantFuture = Instant.DISTANT_FUTURE - 1.nanoseconds
        assertPrints(earlierThanDistantFuture, "+99999-12-31T23:59:59.999999999Z")
        assertPrints(earlierThanDistantFuture.isDistantFuture, "false")
    }

    @Sample
    fun toJavaInstant() {
        // Given a Kotlin Instant.
        val kotlinInstant = Instant.fromEpochMilliseconds(10)
        // It can be converted to a corresponding Java Instant.
        val javaInstant = kotlinInstant.toJavaInstant()

        // The Java Instant will represent the same moment in time,
        // and can be passed to APIs that expect a Java Instant.
        assertPrints(javaInstant.toEpochMilli(), "10")
    }

    @Sample
    fun toKotlinInstant() {
        // Given a Java Instant.
        val javaInstant = java.time.Instant.ofEpochMilli(10)
        // It can be converted to a corresponding Kotlin Instant.
        val kotlinInstant = javaInstant.toKotlinInstant()

        // The Kotlin Instant will represent the same moment in time,
        // and can be passed to APIs that expect a Kotlin Instant.
        assertPrints(kotlinInstant.toEpochMilliseconds(), "10")
    }
}
