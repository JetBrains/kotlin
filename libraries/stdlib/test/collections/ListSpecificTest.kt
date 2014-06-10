package test.collections

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

    Test fun slice() {
        val list = listOf('A', 'B', 'C', 'D')
        // ABCD
        // 0123
        assertEquals(listOf('B', 'C', 'D'), list.slice(1..3))
        assertEquals(listOf('D', 'C', 'B'), list.slice(3 downTo 1))

        val iter = listOf(2, 0, 3)
        assertEquals(listOf('C', 'A', 'D'), list.slice(iter))
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

    Test fun mutableList() {
        val items = listOf("beverage", "location", "name")

        var list = arrayListOf<String>()
        for (item in items) {
            list += item
        }

        assertEquals(3, list.size())
        assertEquals("beverage,location,name", list.join(","))
    }
}
