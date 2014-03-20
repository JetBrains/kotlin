package test.collections

import java.util.ArrayList
import kotlin.test.*
import org.junit.Test

class ListSpecificTest {
    val data = listOf("foo", "bar")
    val empty = listOf<String>()

    Test fun _toString() {
        assertEquals("[foo, bar]", data.toString())
    }

    Test fun tail() {
        val data = arrayListOf("foo", "bar", "whatnot")
        val actual = data.tail
        val expected = arrayListOf("bar", "whatnot")
        assertEquals(expected, actual)
    }

    Test fun utils() {
        assertNull(empty.head)
        assertNull(empty.first)
        assertNull(empty.last)
        assertEquals(-1, empty.lastIndex)

        assertEquals("foo", data.head)
        assertEquals("foo", data.first)
        assertEquals("bar", data.last)
        assertEquals(1, data.lastIndex)
    }
}
