package text

import kotlin.test.Test
import kotlin.test.assertEquals

class AppendableTest {
    @Test
    fun appendableString() {
        assertEquals("foo", appendableString { append("foo") })
    }

    @Test
    fun appendIndented() {
        assertEquals(
                "one\n  two\n  three",
                appendableString { appendIndented { append("one\ntwo\nthree") } })
    }

    @Test
    fun appendFixedSize() {
        assertEquals("123  ", appendableString { appendFixedSize(5) { append("123") } })
        assertEquals("12345", appendableString { appendFixedSize(5) { append("12345667890") } })
    }

    @Test
    fun appendNonISOControl() {
        assertEquals(
                "one.two.three.",
                appendableString { appendNonISOControl { append("one\ntwo\nthree\u0000") } })
    }
}