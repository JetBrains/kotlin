/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.test.assertEquals
import kotlin.time.Duration

fun testDefault(duration: Duration, vararg expected: String) {
    fun testParsing(string: String, expectedDuration: Duration) {
        assertEquals(expectedDuration, Duration.parse(string), string)
        assertEquals(expectedDuration, Duration.parseOrNull(string), string)
    }

    val actual = duration.toString()
    assertEquals(expected.first(), actual)

    if (duration.isPositive()) {
        if (' ' in actual) {
            assertEquals("-($actual)", (-duration).toString())
        } else {
            assertEquals("-$actual", (-duration).toString())
        }
    }

    for (string in expected) {
        testParsing(string, duration)
        if (duration.isPositive() && duration.isFinite()) {
            testParsing("+($string)", duration)
            testParsing("-($string)", -duration)
            if (' ' !in string) {
                testParsing("+$string", duration)
                testParsing("-$string", -duration)
            }
        }
    }
}
