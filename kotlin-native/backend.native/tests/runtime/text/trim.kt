/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.text.trim

import kotlin.test.*

/**
 * Tests correct conversions of floats/doubles along with trimming of leading/trailing whitespaces.
 * String.trim() trims all whitespaces (see kotlin/text/Strings.kt) while String.toFloat() uses
 * the same approach as java.lang.Float.parseFloat()
 */
@Test fun runTest() {
    convertToFloatingPoint()
    convertWithWhitespaces()
    trimWhitespaces()

    println("OK")
}

private fun convertToFloatingPoint() {
    assertEquals(expected = 3.14F, actual = " 3.14  ".toFloat(), message = "String float should be trimmed")
    assertEquals(expected = 3.14, actual = " 3.14  ".toDouble(), message = "String double should be trimmed")
    assertEquals(expected = 7.15F, actual = "\u0019 7.15  ".toFloat(), message = "String float should be trimmed")
    assertEquals(expected = 42.3, actual = "\n 42.3 ".toDouble(), message = "String double should be trimmed")
}

private fun convertWithWhitespaces() {
    val s = "\u0009 \u000A 2.71 \u000D"
    assertEquals(expected = 2.71F, actual = s.toFloat(),
            message = "String should be cleared of LF, CR, TAB and converted to Float")
    assertEquals(expected = 2.71, actual = s.toDouble(),
            message = "String should be cleared of LF, CR, TAB and converted to Double")

    // Special symbols should not be trimmed during String to Float/Double conversion
    assertFailsWith<NumberFormatException> { "\u202F3.14".toFloat() }
    assertFailsWith<NumberFormatException> { "\u20293.14".toDouble() }
    assertFailsWith<NumberFormatException> { "3.14\u200B".toDouble() }
    assertFailsWith<NumberFormatException> { "3.14\u200B ABC".toDouble() }
}

private fun trimWhitespaces() {
    assertEquals(expected = "String", actual = "  String".trim(), message = "Trim leading spaces")
    assertEquals(expected = "String  ", actual = "    String  ".trimStart(), message = "Trim start")
    assertEquals(expected = "  String", actual = "  String \t ".trimEnd(), message = "Trim end")

    assertEquals(expected = "String", actual = "\u0020 \u202FString\u2028\u2029".trim(),
            message = "Trim special whitespaces")
    assertEquals(expected = "\u1FFFString", actual = "\u00A0  \u1FFFString".trim(),
            message = "Trim special whitespace but should left a unicode symbol")
    assertEquals(expected = "String\tSTR", actual = " \nString\tSTR  ".trim(), message = "Trim newline")
}
