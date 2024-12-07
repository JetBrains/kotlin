/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.test.*

// Native-specific part of stdlib/test/numbers/ConversionTest.kt
class ConversionNativeTest {
    @Test
    fun floatToInt() {
        fun testEquals(expected: Int, v: Float) = assertEquals(expected, v.toInt())

        testEquals(3, 3.14f)
        testEquals(-33333, -33333.12312f)
        testEquals(-1, -1.2f)
        testEquals(-12, -12.6f)
        testEquals(2, 2.3f)
    }

    @Test
    fun intToShort() {
        fun testEquals(expected: Short, v: Int) = assertEquals(expected, v.toShort())

        testEquals(-1, Int.MAX_VALUE)
    }
}