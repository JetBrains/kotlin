package samples.text

import samples.*
import kotlin.test.*
import java.util.*

class Chars {
    @Sample
    fun plus() {
        val value = 'a' + "bcd"
        assertPrints(value, "abcd")
    }

    @Sample
    fun equals() {
        assertTrue('a'.equals('a', false))
        assertTrue('a'.equals('A', true))
        assertFalse('a'.equals('A', false))
    }

    @Sample
    fun isSurrogate() {
        assertTrue('\uDF00'.isSurrogate())
        assertFalse('a'.isSurrogate())
    }
}