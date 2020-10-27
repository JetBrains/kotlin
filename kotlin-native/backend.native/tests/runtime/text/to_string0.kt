/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.text.to_string0

import kotlin.test.*

// Based on Apache Harmony tests.

fun assertEquals(actual: String, expected: String, msg: String) {
    if (actual != expected) throw AssertionError("$msg. Actual: $actual. Expected: $expected")
}

fun testIntToStringWithRadix() {
    assertEquals(2147483647.toString(8),  "17777777777", "Octal string")
    assertEquals(2147483647.toString(16), "7fffffff", "Hex string")
    assertEquals(2147483647.toString(2),  "1111111111111111111111111111111", "Binary string")
    assertEquals(2147483647.toString(10), "2147483647", "Decimal string")

    assertEquals((-2147483647).toString(8),  "-17777777777", "Octal string")
    assertEquals((-2147483647).toString(16), "-7fffffff", "Hex string")
    assertEquals((-2147483647).toString(2),  "-1111111111111111111111111111111", "Binary string")
    assertEquals((-2147483647).toString(10), "-2147483647", "Decimal string")

    assertEquals((-2147483648).toString(8),  "-20000000000", "Octal string")
    assertEquals((-2147483648).toString(16), "-80000000", "Hex string")
    assertEquals((-2147483648).toString(2),  "-10000000000000000000000000000000", "Binary string")
    assertEquals((-2147483648).toString(10), "-2147483648", "Decimal string")
}

fun testLongToStringWithRadix() {
    assertEquals(100000000L.toString(10), "100000000", "Decimal string")
    assertEquals(68719476735L.toString(16), "fffffffff", "Hex string")
    assertEquals(8589934591L.toString(8), "77777777777", "Octal string")
    assertEquals(8796093022207L.toString(2), "1111111111111111111111111111111111111111111", "Binary string")

    assertEquals((-0x7fffffffffffffffL - 1).toString(10), "-9223372036854775808", "Min decimal string")
    assertEquals(0x7fffffffffffffffL.toString(10), "9223372036854775807", "Max decimal string")
    assertEquals((-0x7fffffffffffffffL - 1).toString(16), "-8000000000000000", "Min hex string")
    assertEquals(0x7fffffffffffffffL.toString(16), "7fffffffffffffff", "Max hex string")

}

@Test fun runTest() {
    testIntToStringWithRadix()
    testLongToStringWithRadix()
    println("OK")
}