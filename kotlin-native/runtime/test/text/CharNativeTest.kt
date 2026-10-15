
package test.text

import kotlin.test.*

class CharNativeTest {

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
