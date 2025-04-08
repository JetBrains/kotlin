/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import samples.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

class Clocks {
    @Sample
    fun system() {
        // The current instant according to the system clock.
        val currentInstant = Clock.System.now()
        // The number of whole milliseconds that have passed since the Unix epoch.
        println(currentInstant.toEpochMilliseconds())
    }

    @Sample
    fun dependencyInjection() {
        fun formatCurrentTime(clock: Clock): String =
            clock.now().toString()

        // In the production code:
        val currentTimeInProduction = formatCurrentTime(Clock.System)
        // Testing this value is tricky because it changes all the time.
        println(currentTimeInProduction)

        // In the test code:
        val testClock = object : Clock {
            override fun now(): Instant = Instant.parse("2023-01-02T22:35:01Z")
        }
        // Then, one can write a completely deterministic test:
        val currentTimeForTests = formatCurrentTime(testClock)
        assertPrints(currentTimeForTests, "2023-01-02T22:35:01Z")
    }

    @Sample
    fun timeSourceAsClock() {
        // Creating a TimeSource
        // When testing a Clock in combination of kotlinx-coroutines-test, use the testTimeSource of the TestDispatcher.
        val timeSource = TestTimeSource()
        // Creating a clock by setting the specified origin
        val clock = timeSource.asClock(origin = Instant.parse("2023-01-02T22:00:00Z"))

        assertPrints(clock.now(), "2023-01-02T22:00:00Z")

        // Advancing time in the timeSource
        timeSource += 10.seconds

        assertPrints(clock.now(), "2023-01-02T22:00:10Z")
    }
}
