/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseExperimental(ExperimentalTime::class)
package test.time

import kotlin.test.*
import kotlin.time.*

class TestTimeSourceTest {

    @Test
    fun overflows() {
        for (enormousDuration in listOf(Duration.INFINITE, Double.MAX_VALUE.nanoseconds, Long.MAX_VALUE.nanoseconds * 2)) {
            assertFailsWith<IllegalStateException>(enormousDuration.toString()) { TestTimeSource() += enormousDuration }
            assertFailsWith<IllegalStateException>((-enormousDuration).toString()) { TestTimeSource() += -enormousDuration }
        }

        val moderatePositiveDuration = Long.MAX_VALUE.takeHighestOneBit().nanoseconds
        val borderlinePositiveDuration = Long.MAX_VALUE.nanoseconds // rounded to 2.0^63, which is slightly more than Long.MAX_VALUE
        val borderlineNegativeDuration = Long.MIN_VALUE.nanoseconds
        run {
            val timeSource = TestTimeSource()
            timeSource += moderatePositiveDuration
            assertFailsWith<IllegalStateException>("Should overflow positive") { timeSource += moderatePositiveDuration }
        }
        run {
            val timeSource = TestTimeSource()
            timeSource += borderlinePositiveDuration
            assertFailsWith<IllegalStateException>("Should overflow positive") { timeSource += 1.nanoseconds }
        }
        run {
            val timeSource = TestTimeSource()
            timeSource += borderlineNegativeDuration
            assertFailsWith<IllegalStateException>("Should overflow negative") { timeSource += -1.nanoseconds }
        }

        run {
            val timeSource = TestTimeSource()
            timeSource += moderatePositiveDuration
            // does not overflow event if duration doesn't fit in long
            timeSource += -moderatePositiveDuration + borderlineNegativeDuration
        }
    }

    @Test
    fun nanosecondRounding() {
        val timeSource = TestTimeSource()
        val mark = timeSource.markNow()

        repeat(10_000) {
            timeSource += 0.9.nanoseconds

            assertEquals(Duration.ZERO, mark.elapsedNow())
        }

        timeSource += 1.9.nanoseconds
        assertEquals(1.nanoseconds, mark.elapsedNow())
    }
}