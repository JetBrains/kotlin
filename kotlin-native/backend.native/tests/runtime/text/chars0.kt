/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.text.chars0

import kotlin.test.*

fun testIsSupplementaryCodePoint() {
    assertFalse(Char.isSupplementaryCodePoint(-1))
    for (c in 0..0xFFFF) {
        assertFalse(Char.isSupplementaryCodePoint(c.toInt()))
    }
    for (c in 0xFFFF + 1..0x10FFFF) {
        assertTrue(Char.isSupplementaryCodePoint(c))
    }
    assertFalse(Char.isSupplementaryCodePoint(0x10FFFF + 1))
}

fun testIsSurrogatePair() {
    assertFalse(Char.isSurrogatePair('\u0000', '\u0000'))
    assertFalse(Char.isSurrogatePair('\u0000', '\uDC00'))
    assertTrue( Char.isSurrogatePair('\uD800', '\uDC00'))
    assertTrue( Char.isSurrogatePair('\uD800', '\uDFFF'))
    assertTrue( Char.isSurrogatePair('\uDBFF', '\uDFFF'))
    assertFalse(Char.isSurrogatePair('\uDBFF', '\uF000'))
}

fun testToChars() {
    assertTrue(charArrayOf('\uD800', '\uDC00').contentEquals(Char.toChars(0x010000)))
    assertTrue(charArrayOf('\uD800', '\uDC01').contentEquals(Char.toChars(0x010001)))
    assertTrue(charArrayOf('\uD801', '\uDC01').contentEquals(Char.toChars(0x010401)))
    assertTrue(charArrayOf('\uDBFF', '\uDFFF').contentEquals(Char.toChars(0x10FFFF)))

    try {
        Char.toChars(Int.MAX_VALUE)
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}
}

fun testToCodePoint() {
    assertEquals(0x010000, Char.toCodePoint('\uD800', '\uDC00'))
    assertEquals(0x010001, Char.toCodePoint('\uD800', '\uDC01'))
    assertEquals(0x010401, Char.toCodePoint('\uD801', '\uDC01'))
    assertEquals(0x10FFFF, Char.toCodePoint('\uDBFF', '\uDFFF'))
}

// TODO: Uncomment when such operations are supported for supplementary codepoints and the API is public.
fun testCase() {
    /*
    assertEquals('A'.toInt(), Char.toUpperCase('a'.toInt()))
    assertEquals('A'.toInt(), Char.toUpperCase('A'.toInt()))
    assertEquals('1'.toInt(), Char.toUpperCase('1'.toInt()))

    assertEquals('a'.toInt(), Char.toLowerCase('A'.toInt()))
    assertEquals('a'.toInt(), Char.toLowerCase('a'.toInt()))
    assertEquals('1'.toInt(), Char.toLowerCase('1'.toInt()))

    assertEquals(0x010400, Char.toUpperCase(0x010428))
    assertEquals(0x010400, Char.toUpperCase(0x010400))
    assertEquals(0x10FFFF, Char.toUpperCase(0x10FFFF))
    assertEquals(0x110000, Char.toUpperCase(0x110000))

    assertEquals(0x010428, Char.toLowerCase(0x010400))
    assertEquals(0x010428, Char.toLowerCase(0x010428))
    assertEquals(0x10FFFF, Char.toLowerCase(0x10FFFF))
    assertEquals(0x110000, Char.toLowerCase(0x110000))
    */
}

fun testCategory() {
    assertTrue('\n'     in CharCategory.CONTROL)
    assertTrue('1'      in CharCategory.DECIMAL_DIGIT_NUMBER)
    assertTrue(' '      in CharCategory.SPACE_SEPARATOR)
    assertTrue('a'      in CharCategory.LOWERCASE_LETTER)
    assertTrue('A'      in CharCategory.UPPERCASE_LETTER)
    assertTrue('<'      in CharCategory.MATH_SYMBOL)
    assertTrue(';'      in CharCategory.OTHER_PUNCTUATION)
    assertTrue('_'      in CharCategory.CONNECTOR_PUNCTUATION)
    assertTrue('$'      in CharCategory.CURRENCY_SYMBOL)
    assertTrue('\u2029' in CharCategory.PARAGRAPH_SEPARATOR)
}

fun testIsHighSurrogate() {
    assertTrue('\uD800'.isHighSurrogate())
    assertTrue('\uDBFF'.isHighSurrogate())
    assertFalse('\uDC00'.isHighSurrogate())
    assertFalse('\uDFFF'.isHighSurrogate())
}

fun testIsLowSurrogate() {
    assertFalse('\uD800'.isLowSurrogate())
    assertFalse('\uDBFF'.isLowSurrogate())
    assertTrue('\uDC00'.isLowSurrogate())
    assertTrue('\uDFFF'.isLowSurrogate())
}

@Test fun runTest() {
    testIsSurrogatePair()
    testToChars()
    testToCodePoint()
    testIsSupplementaryCodePoint()
    testCase()
    testCategory()
    testIsHighSurrogate()
    testIsLowSurrogate()
}