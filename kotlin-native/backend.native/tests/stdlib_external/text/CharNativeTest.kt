
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

    fun titlecase() {
        // titlecase = titlecaseChar = char != uppercaseChar
        assertEquals('\u10F0'.titlecaseChar().toString(), '\u10F0'.titlecase())
        assertEquals('\u10F0', '\u10F0'.titlecaseChar())
        assertNotEquals('\u10F0', '\u10F0'.uppercaseChar())
    }
}
