/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseExperimental(ExperimentalTime::class)
package test.time

import kotlin.test.*
import kotlin.time.*

class TestClockTest {

    @Test
    fun overflows() {
        for (enormousDuration in listOf(Duration.INFINITE, Double.MAX_VALUE.nanoseconds, Long.MAX_VALUE.nanoseconds * 2)) {
            assertFailsWith<IllegalStateException>(enormousDuration.toString()) { TestClock() += enormousDuration }
            assertFailsWith<IllegalStateException>((-enormousDuration).toString()) { TestClock() += -enormousDuration }
        }

        val moderatePositiveDuration = Long.MAX_VALUE.takeHighestOneBit().nanoseconds
        val borderlinePositiveDuration = Long.MAX_VALUE.nanoseconds // rounded to 2.0^63, which is slightly more than Long.MAX_VALUE
        val borderlineNegativeDuration = Long.MIN_VALUE.nanoseconds
        run {
            val clock = TestClock()
            clock += moderatePositiveDuration
            assertFailsWith<IllegalStateException>("Should overflow positive") { clock += moderatePositiveDuration }
        }
        run {
            val clock = TestClock()
            clock += borderlinePositiveDuration
            assertFailsWith<IllegalStateException>("Should overflow positive") { clock += 1.nanoseconds }
        }
        run {
            val clock = TestClock()
            clock += borderlineNegativeDuration
            assertFailsWith<IllegalStateException>("Should overflow negative") { clock += -1.nanoseconds }
        }

        run {
            val clock = TestClock()
            clock += moderatePositiveDuration
            // does not overflow event if duration doesn't fit in long
            clock += -moderatePositiveDuration + borderlineNegativeDuration
        }
    }

    @Test
    fun nanosecondRounding() {
        val clock = TestClock()
        val mark = clock.markNow()

        repeat(10_000) {
            clock += 0.9.nanoseconds

            assertEquals(Duration.ZERO, mark.elapsedNow())
        }

        clock += 1.9.nanoseconds
        assertEquals(1.nanoseconds, mark.elapsedNow())
    }
}