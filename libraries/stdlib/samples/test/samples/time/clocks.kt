/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.time

import samples.*
import kotlin.time.*

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
}
