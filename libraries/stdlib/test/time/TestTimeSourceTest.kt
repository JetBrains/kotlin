/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class)
package test.time

import kotlin.test.*
import kotlin.time.*

class TestTimeSourceTest {

    @Test
    fun overflows() {
        for (enormousDuration in listOf(Duration.INFINITE, Duration.nanoseconds(Double.MAX_VALUE), Duration.nanoseconds(Long.MAX_VALUE) * 2)) {
            assertFailsWith<IllegalStateException>(enormousDuration.toString()) { TestTimeSource() += enormousDuration }
            assertFailsWith<IllegalStateException>((-enormousDuration).toString()) { TestTimeSource() += -enormousDuration }
        }

        val moderatePositiveDuration = Duration.nanoseconds(Long.MAX_VALUE) / 1.5
        val borderlineQuarterPositiveDuration = Duration.nanoseconds(Long.MAX_VALUE / 4) // precise number of ns
        val borderlineQuarterNegativeDuration = Duration.nanoseconds(Long.MIN_VALUE / 4)
        run {
            val timeSource = TestTimeSource()
            timeSource += moderatePositiveDuration
            assertFailsWith<IllegalStateException>("Should overflow positive") { timeSource += moderatePositiveDuration }
        }
        run {
            val timeSource = TestTimeSource()
            repeat(4) { timeSource += borderlineQuarterPositiveDuration }
            assertFailsWith<IllegalStateException>("Should overflow positive") { timeSource += Duration.nanoseconds(4) }
        }
        run {
            val timeSource = TestTimeSource()
            repeat(4) { timeSource += borderlineQuarterNegativeDuration }
            assertFailsWith<IllegalStateException>("Should overflow negative") { timeSource += -Duration.nanoseconds(4) }
        }

        run {
            val timeSource = TestTimeSource()
            timeSource += moderatePositiveDuration
            // does not overflow even if duration doesn't fit in long, but the result fits
            timeSource += -moderatePositiveDuration - Duration.nanoseconds(Long.MAX_VALUE)
        }
    }

    @Test
    fun nanosecondRounding() {
        val timeSource = TestTimeSource()
        val mark = timeSource.markNow()

        repeat(10_000) {
            timeSource += Duration.nanoseconds(0.9)

            assertEquals(Duration.ZERO, mark.elapsedNow())
        }

        timeSource += Duration.nanoseconds(1.9)
        assertEquals(Duration.nanoseconds(1), mark.elapsedNow())
    }
}
