/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds

class TestTimeSourceTest {

    @Test
    fun overflows() {
        for (enormousDuration in listOf(Duration.INFINITE, Double.MAX_VALUE.nanoseconds, Long.MAX_VALUE.nanoseconds * 2)) {
            assertFailsWith<IllegalStateException>(enormousDuration.toString()) { TestTimeSource() += enormousDuration }
            assertFailsWith<IllegalStateException>((-enormousDuration).toString()) { TestTimeSource() += -enormousDuration }
        }

        val moderatePositiveDuration = Long.MAX_VALUE.nanoseconds / 1.5
        val borderlineQuarterPositiveDuration = (Long.MAX_VALUE / 4).nanoseconds // precise number of ns
        val borderlineQuarterNegativeDuration = (Long.MIN_VALUE / 4).nanoseconds
        run {
            val timeSource = TestTimeSource()
            timeSource += moderatePositiveDuration
            assertFailsWith<IllegalStateException>("Should overflow positive") { timeSource += moderatePositiveDuration }
        }
        run {
            val timeSource = TestTimeSource()
            repeat(4) { timeSource += borderlineQuarterPositiveDuration }
            assertFailsWith<IllegalStateException>("Should overflow positive") { timeSource += 4.nanoseconds }
        }
        run {
            val timeSource = TestTimeSource()
            repeat(4) { timeSource += borderlineQuarterNegativeDuration }
            assertFailsWith<IllegalStateException>("Should overflow negative") { timeSource += -4.nanoseconds }
        }

        run {
            val timeSource = TestTimeSource()
            timeSource += moderatePositiveDuration
            val mark = timeSource.markNow()
            // does not overflow even if duration doesn't fit in long, but the result fits
            timeSource += -moderatePositiveDuration - Long.MAX_VALUE.nanoseconds
            assertEquals(-(moderatePositiveDuration + Long.MAX_VALUE.nanoseconds), mark.elapsedNow())
        }
    }

    @Test
    fun nanosecondRounding() {
        val timeSource = TestTimeSource()
        val mark = timeSource.markNow()

        repeat(10_000) {
            timeSource += 0.4.nanoseconds

            assertEquals(Duration.ZERO, mark.elapsedNow())
        }

        timeSource += 1.9.nanoseconds
        assertEquals(2.nanoseconds, mark.elapsedNow())
    }
}
