/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk8.time.test

import kotlin.random.Random
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import java.time.Duration as JTDuration

class DurationConversionTest {
    @Test
    fun twoWayConversion() {
        fun test(days: Int, hours: Int, minutes: Int, seconds: Int, millis: Int, nanos: Int) {
            val duration = with(Duration) {
                days.days + hours.hours + minutes.minutes + seconds.seconds + millis.milliseconds + nanos.nanoseconds
            }
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
        val jtDuration1 = JTDuration.ofDays(365L * 150)
        val jtDuration2 = jtDuration1.plusNanos(1)
        assertNotEquals(jtDuration1, jtDuration2)

        val duration1 = jtDuration1.toKotlinDuration()
        val duration2 = jtDuration1.toKotlinDuration()
        assertEquals(duration1, duration2)
        assertEquals((365 * 150).days, duration2)
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
            val duration = Random.nextLong(-(1L shl 53) + 1, 1L shl 53).nanoseconds
            val fromString = JTDuration.parse(duration.toIsoString())
            val fromDuration = duration.toJavaDuration()

            assertEquals(fromString, fromDuration)
        }
    }
}

