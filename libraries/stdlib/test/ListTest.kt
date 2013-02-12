package test.collections

import java.util.ArrayList
import kotlin.test.*
import org.junit.Test as test

class ListTest {

    test fun toString() {
        val data = arrayList("foo", "bar")
        assertEquals("[foo, bar]", data.toString())
    }

    test fun head() {
        val data = arrayList("foo", "bar")
        assertEquals("foo", data.head)
    }

    test fun tail() {
        val data = arrayList("foo", "bar", "whatnot")
        val actual = data.tail
        val expected = arrayList("bar", "whatnot")
        assertEquals(expected, actual)
    }

    test fun first() {
        val data = arrayList("foo", "bar")
        assertEquals("foo", data.first)
    }

    test fun last() {
        val data = arrayList("foo", "bar")
        assertEquals("bar", data.last)
    }

    test fun forEachWithIndex() {
        val data = arrayList("foo", "bar")
        var index = 0

        data.forEachWithIndex { (i, d) ->
            assertEquals(i, index)
            assertEquals(d, data[index])
            index++
        }

        assertEquals(data.size(), index)
    }

    test fun withIndices() {
        val data = arrayList("foo", "bar")
        var index = 0

        for ((i, d) in data.withIndices()) {
            assertEquals(i, index)
            assertEquals(d, data[index])
            index++
        }

        assertEquals(data.size(), index)
    }

    test fun lastIndex() {
        val emptyData = ArrayList<String>()
        val data = arrayList("foo", "bar")

        assertEquals(-1, emptyData.lastIndex)
        assertEquals(1, data.lastIndex)
    }
}
