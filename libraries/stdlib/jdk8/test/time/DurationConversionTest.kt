/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseExperimental(ExperimentalTime::class)

package kotlin.jdk8.time.test

import kotlin.random.Random
import kotlin.test.*
import kotlin.time.*
import java.time.Duration as JTDuration

class DurationConversionTest {
    @Test
    fun twoWayConversion() {
        fun test(days: Int, hours: Int, minutes: Int, seconds: Int, millis: Int, nanos: Int) {
            val duration = days.days + hours.hours + minutes.minutes + seconds.seconds + millis.milliseconds + nanos.nanoseconds
            val jtDuration = JTDuration.ZERO
                .plusDays(days.toLong())
                .plusHours(hours.toLong())
                .plusMinutes(minutes.toLong())
                .plusSeconds(seconds.toLong())
                .plusMillis(millis.toLong())
                .plusNanos(nanos.toLong())

            assertEquals(jtDuration, duration.toJavaDuration())
            assertEquals(duration, jtDuration.toKotlinDuration())
        }

        repeat(100) {
            test(
                days = Random.nextInt(-100, 100),
                hours = Random.nextInt(-48, 48),
                minutes = Random.nextInt(-600, 600),
                seconds = Random.nextInt(-600, 600),
                millis = Random.nextInt(-1000000, 1000000),
                nanos = Random.nextInt()
            )
        }
    }

    @Test
    fun javaToKotlinRounding() {
        val jtDuration = JTDuration.ofDays(105).plusNanos(1)
        val duration = jtDuration.toKotlinDuration()
        assertEquals(105.days, duration)
    }

    @Test
    fun kotlinToJavaClamping() {
        val duration = Long.MAX_VALUE.seconds * 5
        val jtDuration = duration.toJavaDuration()
        assertEquals(JTDuration.ofSeconds(Long.MAX_VALUE), jtDuration)

        val jtnegDuration = (-duration).toJavaDuration()
        assertEquals(JTDuration.ofSeconds(Long.MIN_VALUE), jtnegDuration)
    }

    @Test
    fun randomIsoConversionEquivalence() {
        repeat(100) {
            val duration = Random.nextLong(-(1 shl 53) + 1, 1 shl 53).nanoseconds
            val fromString = JTDuration.parse(duration.toIsoString())
            val fromDuration = duration.toJavaDuration()

            assertEquals(fromString, fromDuration)
        }
    }
}

