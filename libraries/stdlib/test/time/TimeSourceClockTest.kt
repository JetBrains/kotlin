/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds


class TimeSourceClockTest {

    @Test
    fun timeSourceAsClock() {
        val timeSource = TestTimeSource()
        val clock = timeSource.asClock(origin = Instant.fromEpochSeconds(0))

        assertEquals(Instant.fromEpochSeconds(0), clock.now())
        assertEquals(Instant.fromEpochSeconds(0), clock.now())

        timeSource += 1.seconds
        assertEquals(Instant.fromEpochSeconds(1), clock.now())
        assertEquals(Instant.fromEpochSeconds(1), clock.now())
    }

    @Test
    fun syncMultipleClocksFromTimeSource() {
        val timeSource = TestTimeSource()
        val clock1 = timeSource.asClock(origin = Instant.fromEpochSeconds(0))

        assertEquals(0, clock1.now().epochSeconds)

        timeSource += 1.seconds
        assertEquals(1, clock1.now().epochSeconds)

        val clock2 = timeSource.asClock(origin = Instant.fromEpochSeconds(1))
        assertEquals(clock1.now(), clock2.now())

        timeSource += 1.seconds
        assertEquals(2, clock1.now().epochSeconds)
        assertEquals(clock1.now(), clock2.now())

        val clock3 = timeSource.asClock(origin = clock2.now())
        timeSource += 1.seconds
        assertEquals(3, clock3.now().epochSeconds)
        assertEquals(clock1.now(), clock2.now())
        assertEquals(clock1.now(), clock3.now())
    }
}