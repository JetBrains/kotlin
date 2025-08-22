/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import test.time.testDefault
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class DurationTest {

    @Test
    fun nanosecondsRounding() {
        for (i in 0..4) {
            testDefault(Duration.ZERO, "0s", "0.000000000${i}s", "0.000000${i}ms", "0.000${i}us", "0.${i}ns")
            testDefault(1.nanoseconds, "1ns", "0.000000000${i + 5}s", "0.000000${i + 5}ms", "0.000${i + 5}us", "0.${i + 5}ns")
            testDefault(1.seconds - 1.nanoseconds, "999.999999ms", "0.999999999${i}s")
            testDefault(1.seconds, "1s", "0.999999999${i + 5}s")
            testDefault(1.milliseconds - 1.nanoseconds, "999.999us", "0.999999${i}ms")
            testDefault(1.milliseconds, "1ms", "0.999999${i + 5}ms")
            testDefault(1.microseconds - 1.nanoseconds, "999ns", "0.999${i}us")
            testDefault(1.microseconds, "1us", "0.999${i + 5}us")
        }

        testDefault(1.minutes - 1.nanoseconds, "59.999999999s", "0.999999999991666610m")
        for (i in 1..9) {
            testDefault(1.minutes, "1m", "0.99999999999166661${i}m")
        }

        for (i in 0..1) {
            testDefault(1.days - 1.nanoseconds, "23h 59m 59.999999999s", "0.99999999999999417${i}d")
        }
        for (i in 2..9) {
            testDefault(1.days, "1d", "0.99999999999999417${i}d")
        }

        for (i in 0..3) {
            testDefault(Duration.ZERO, "0s", "0.0000000000000057870370370370${i}d", "0.0000000000083333333333333${i}m")
        }
        for (i in 4..9) {
            testDefault(1.nanoseconds, "1ns", "0.0000000000000057870370370370${i}d", "0.0000000000083333333333333${i}m")
        }

        for (i in 0..5) {
            testDefault(Duration.ZERO, "0s", "0.00000000000000${i}d")
            testDefault(1.hours - 1.nanoseconds, "59m 59.999999999s", "0.99999999999986105${i}h")
        }
        for (i in 6..9) {
            testDefault(1.nanoseconds, "1ns", "0.00000000000000${i}d")
            testDefault(1.hours, "1h", "0.99999999999986105${i}h")
        }

        for (i in 0..8) {
            testDefault(Duration.ZERO, "0s", "0.00000000000013${i}h", "0.000000000000138888888888888${i}h")
        }
        testDefault(1.nanoseconds, "1ns", "0.000000000000139h", "0.0000000000001388888888888889h")
    }
}
