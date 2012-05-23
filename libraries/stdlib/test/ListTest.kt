package test.collections

import kotlin.test.*
import org.junit.Test as test

class ListTest {

    test fun head() {
        val data = arrayList("foo", "bar")
        assertEquals("foo", data.head)
    }

    test fun tail() {
        val data = arrayList("foo", "bar", "whatnot")
        assertEquals(arrayList("bar", "whatnot"), data.tail)
    }

    test fun first() {
        val data = arrayList("foo", "bar")
        assertEquals("foo", data.first)
    }

    test fun last() {
        val data = arrayList("foo", "bar")
        assertEquals("bar", data.last)
    }

    test fun withIndices() {
        val data = arrayList("foo", "bar")
        val withIndices = data.withIndices()
        var index = 0
        for (withIndex in withIndices) {
            assertEquals(withIndex._1, index)
            assertEquals(withIndex._2, data[index])
            index++
        }
        assertEquals(data.size(), index)
    }
}
