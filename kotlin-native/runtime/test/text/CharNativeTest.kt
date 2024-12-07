
package test.text

import kotlin.test.*

class CharNativeTest {

    @Test
    fun lowercaseChar() {
        // large mapping
        assertEquals('\u0239', '\u0239'.lowercaseChar())
        assertEquals('\u2C65', '\u023A'.lowercaseChar())
        assertEquals('\u023C', '\u023B'.lowercaseChar())

        // large negative mapping
        assertEquals('\u2C7D', '\u2C7D'.lowercaseChar())
        assertEquals('\u023F', '\u2C7E'.lowercaseChar())
        assertEquals('\u0240', '\u2C7F'.lowercaseChar())

        // <Lu, Ll>
        assertEquals('\u2C81', '\u2C80'.lowercaseChar())
        assertEquals('\u2C81', '\u2C81'.lowercaseChar())
        assertEquals('\u2C83', '\u2C82'.lowercaseChar())
    }

    @Test
    fun uppercaseChar() {
        // large mapping
        assertEquals('\u029C', '\u029C'.uppercaseChar())
        assertEquals('\uA7B2', '\u029D'.uppercaseChar())
        assertEquals('\uA7B0', '\u029E'.uppercaseChar())
        assertEquals('\u029F', '\u029F'.uppercaseChar())

        // large negative mapping
        assertEquals('\uAB6F', '\uAB6F'.uppercaseChar())
        assertEquals('\u13A0', '\uAB70'.uppercaseChar())
        assertEquals('\u13EF', '\uABBF'.uppercaseChar())
        assertEquals('\uABC0', '\uABC0'.uppercaseChar())
    }

    @Test
    fun titlecaseChar() {
        // titlecaseChar == char && uppercaseChar != char
        assertEquals('\u10CF'.uppercaseChar(), '\u10CF'.titlecaseChar())
        for (char in '\u10D0'..'\u10FA') {
            assertEquals(char, char.titlecaseChar())
            assertNotEquals(char, char.uppercaseChar())
        }
        for (char in '\u10FB'..'\u10FC') {
            assertEquals(char, char.titlecaseChar())
            assertEquals(char, char.uppercaseChar())
        }
        for (char in '\u10FD'..'\u10FF') {
            assertEquals(char, char.titlecaseChar())
            assertNotEquals(char, char.uppercaseChar())
        }
        assertEquals('\u1100'.uppercaseChar(), '\u1100'.titlecaseChar())
    }

    @Test
    fun lowercase() {
        // LATIN CAPITAL LETTER I WITH DOT ABOVE
        assertEquals("\u0069\u0307", '\u0130'.lowercase())
    }

    @Test
    fun titlecase() {
        // titlecase = titlecaseChar = char != uppercaseChar
        assertEquals('\u10F0'.titlecaseChar().toString(), '\u10F0'.titlecase())
        assertEquals('\u10F0', '\u10F0'.titlecaseChar())
        assertNotEquals('\u10F0', '\u10F0'.uppercaseChar())
    }

    @Test
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

    @Test
    fun isSurrogatePair() {
        assertFalse(Char.isSurrogatePair('\u0000', '\u0000'))
        assertFalse(Char.isSurrogatePair('\u0000', '\uDC00'))
        assertTrue( Char.isSurrogatePair('\uD800', '\uDC00'))
        assertTrue( Char.isSurrogatePair('\uD800', '\uDFFF'))
        assertTrue( Char.isSurrogatePair('\uDBFF', '\uDFFF'))
        assertFalse(Char.isSurrogatePair('\uDBFF', '\uF000'))
    }

    @Test
    fun toChars() {
        assertContentEquals(Char.toChars(0x010000), charArrayOf('\uD800', '\uDC00'))
        assertContentEquals(Char.toChars(0x010001), charArrayOf('\uD800', '\uDC01'))
        assertContentEquals(Char.toChars(0x010401), charArrayOf('\uD801', '\uDC01'))
        assertContentEquals(Char.toChars(0x10FFFF), charArrayOf('\uDBFF', '\uDFFF'))

        assertFailsWith<IllegalArgumentException> { Char.toChars(Int.MAX_VALUE) }
    }

    @Test
    fun toCodePoint() {
        assertEquals(0x010000, Char.toCodePoint('\uD800', '\uDC00'))
        assertEquals(0x010001, Char.toCodePoint('\uD800', '\uDC01'))
        assertEquals(0x010401, Char.toCodePoint('\uD801', '\uDC01'))
        assertEquals(0x10FFFF, Char.toCodePoint('\uDBFF', '\uDFFF'))
    }

    @Test
    fun category() {
        assertTrue('<'      in CharCategory.MATH_SYMBOL)
        assertTrue(';'      in CharCategory.OTHER_PUNCTUATION)
        assertTrue('_'      in CharCategory.CONNECTOR_PUNCTUATION)
        assertTrue('$'      in CharCategory.CURRENCY_SYMBOL)
    }

    @Test
    fun testToString() {
        assertEquals("A", 'A'.toString())
        assertEquals("Ё", 'Ё'.toString())
        assertEquals("ト", 'ト'.toString())
    }
}
