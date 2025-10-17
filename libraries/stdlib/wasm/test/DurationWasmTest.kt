/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import test.time.testDefaultParsing
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Dedicated test class for Duration parsing on wasmJs platform.
 *
 * These tests are separate from the main Duration tests because Wasm uses its own
 * floating-point arithmetic internally, which can produce different rounding behavior
 * compared to JVM or Native platforms. The differences are particularly noticeable when
 * parsing fractional durations that approach the limits of floating-point precision.
 *
 * The tests here verify that Duration parsing correctly handles edge cases where
 * Wasm's number representation might cause values to round differently than
 * on other platforms, ensuring consistent behavior across all Kotlin targets.
 */
class DurationWasmTest {

    @Test
    fun nanosecondsRounding() {
        testDefaultParsing(1.minutes - 1.nanoseconds, "59.999999999s", "0.999999999991666610m")
        for (i in 1..9) {
            testDefaultParsing(1.minutes, "1m", "0.99999999999166661${i}m")
        }

        for (i in 0..1) {
            testDefaultParsing(1.days - 1.nanoseconds, "23h 59m 59.999999999s", "0.99999999999999417${i}d")
        }
        for (i in 2..9) {
            testDefaultParsing(1.days, "1d", "0.99999999999999417${i}d")
        }

        for (i in 0..3) {
            testDefaultParsing(Duration.ZERO, "0s", "0.0000000000000057870370370370${i}d", "0.0000000000083333333333333${i}m")
        }
        for (i in 4..9) {
            testDefaultParsing(1.nanoseconds, "1ns", "0.0000000000000057870370370370${i}d", "0.0000000000083333333333333${i}m")
        }

        for (i in 0..5) {
            testDefaultParsing(Duration.ZERO, "0s", "0.00000000000000${i}d")
            testDefaultParsing(1.hours - 1.nanoseconds, "59m 59.999999999s", "0.99999999999986105${i}h")
        }
        for (i in 6..9) {
            testDefaultParsing(1.nanoseconds, "1ns", "0.00000000000000${i}d")
            testDefaultParsing(1.hours, "1h", "0.99999999999986105${i}h")
        }

        for (i in 0..8) {
            testDefaultParsing(Duration.ZERO, "0s", "0.000000000000138888888888888${i}h")
        }
        testDefaultParsing(1.nanoseconds, "1ns", "0.0000000000001388888888888889h")
    }
}
