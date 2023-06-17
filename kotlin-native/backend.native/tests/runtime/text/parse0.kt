/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package runtime.text.parse0

import kotlin.test.*
import kotlin.native.OsFamily
import kotlin.native.Platform

@Test
fun runTest() {
    assertEquals(false, "false".toBoolean())
    assertEquals(true, "true".toBoolean())

    assertEquals(-1, "-1".toByte())
    assertEquals(10, "a".toByte(16))

    assertEquals(170, "aa".toShort(16))
    assertEquals(30, "11110".toInt(2))

    assertEquals(4294967295, "ffffffff".toLong(16))

    assertFailsWith<NumberFormatException> {
        "ffffffff".toLong(10)
    }
}

@Test
fun checkDouble() {
    // ===== toDouble() parsing =======
    assertEquals(0.5, "0.5".toDouble())
    assertEquals(-5000000000.0, "-00000000000000000000.5e10".toDouble())
    assertEquals(-0.005, "-00000000000000000000.5e-2".toDouble())
    assertEquals(50000000000.0, "+5e10".toDouble())
    assertEquals(50000000000.0, "   +5e10   ".toDouble())
    assertEquals(0.052, "+5.2e-2".toDouble())
    assertEquals(520.0, "+5.2e2d".toDouble())
    assertEquals(0.052, "+5.2e-2d".toDouble())
    assertEquals(52340000000.0, "+5.234e+10d".toDouble())
    assertEquals(5.234E123, "+5.234e+123d".toDouble())
    assertEquals(5.234E123, "+5.234e+123f".toDouble())
    assertEquals(5.234E123, "+5.234e+123".toDouble())
    assertEquals(5.5, "5.5f".toDouble())

    assertEquals(1.0 / 0.0, "+Infinity".toDouble())
    assertEquals(1.0 / 0.0, "Infinity".toDouble())
    assertEquals(-1.0 / 0.0, "-Infinity".toDouble())
    assertTrue("Infinity".toDouble().isInfinite(), "Infinity is expected for parsing Infinity")

    assertTrue("+NaN".toDouble().isNaN(), "NaN is expected for parsing +NaN")
    assertTrue("NaN".toDouble().isNaN(), "NaN is expected for parsing NaN")
    assertTrue("-NaN".toDouble().isNaN(), "NaN is expected for parsing -NaN")

    if (Platform.osFamily != OsFamily.WASM) {
        assertFailsWith<NumberFormatException> {
            "+-5.0".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "d".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "5.5.3e123d".toDouble()
        }

        // regression of incorrect processing of long lines - such values returned Infinity
        assertFailsWith<NumberFormatException> {
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "+-my free text           with different letters $3213#.  e ".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "eeeeeEEEEEeeeeeee".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "InfinityN".toDouble()
        }
        assertFailsWith<NumberFormatException> {
            "NaNPICEZy".toDouble()
        }
    }
}

@Test
fun checkFloat() {
    // ===== toFloat() parsing =======
    assertEquals(0.5f, "0.5".toFloat())
    assertEquals(-5000000000f, "-00000000000000000000.5e10f".toFloat())
    assertEquals(-0.005f, "-00000000000000000000.5e-2f".toFloat())
    assertEquals(50000000000f, "+5e10".toFloat())
    assertEquals(50000000000f, "    +5e10    ".toFloat())
    assertEquals(0.052f, "+5.2e-2f".toFloat())
    assertEquals(520f, "+5.2e2f".toFloat())
    assertEquals(0.052f, "+5.2e-2f".toFloat())
    assertEquals(52340000000f, "+5.234e+10f".toFloat())
    assertEquals(1.0F / 0.0F, "+5.234e+123f".toFloat())


    assertEquals(1.0F / 0.0F, "+Infinity".toFloat())
    assertEquals(1.0F / 0.0F, "Infinity".toFloat())
    assertEquals(-1.0F / 0.0F, "-Infinity".toFloat())
    assertTrue("Infinity".toFloat().isInfinite(), "Infinity is expected for parsing Infinity")

    assertTrue("+NaN".toFloat().isNaN(), "NaN is expected for parsing +NaN")
    assertTrue("NaN".toFloat().isNaN(), "NaN is expected for parsing NaN")
    assertTrue("-NaN".toFloat().isNaN(), "NaN is expected for parsing -NaN")

    if (Platform.osFamily != OsFamily.WASM) {
        assertFailsWith<NumberFormatException> {
            "+-5.0f".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            "f".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            "5.5.3e123f".toFloat()
        }

        // regression of incorrect processing of long lines - such values returned Infinity
        assertFailsWith<NumberFormatException> {
            // should be more than 38 symbols
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            // should be more than 38 symbols
            "this string is not a numb3r, am I right?????????????".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            // should be more than 38 symbols
            "+-my free text           with different letters $3213#.  e ".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            // should be more than 38 symbols
            "eeeeeEEEEEeeeeeee".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            "InfinityN".toFloat()
        }
        assertFailsWith<NumberFormatException> {
            "NaNPICEZy".toFloat()
        }
    }
}