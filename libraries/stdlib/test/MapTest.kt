package test.collections

import kotlin.test.*

// TODO can we avoid importing all this stuff by default I wonder?
// e.g. making println and the collection builder methods public by default?
import kotlin.*
import kotlin.io.*
import kotlin.util.*
import java.util.*
import junit.framework.TestCase

class MapTest() : TestCase() {
    val data: java.util.Map<String, Int> = java.util.HashMap<String, Int>()

    fun testGetOrElse() {
        val a = data.getOrElse("foo"){2}
        assertEquals(2, a)

        val b = data.getOrElse("foo"){3}
        assertEquals(3, b)
        assertEquals(0, data.size())
    }

    fun testGetOrPut() {
        val a = data.getOrPut("foo"){2}
        assertEquals(2, a)

        val b = data.getOrPut("foo"){3}
        assertEquals(2, b)

        assertEquals(1, data.size())
    }

    fun testSizeAndEmpty() {
        assertTrue{ data.empty }
        // TODO using size breaks a test case
        assertEquals(data.size(), 0)
    }

    fun testSetViaIndexOperators() {
        val map = java.util.HashMap<String, String>()
        assertTrue{ map.empty }
        // TODO using size breaks a test case
        assertEquals(map.size(), 0)

        map["name"] = "James"

        assertTrue{ !map.empty }
        assertEquals(map.size(), 1)
        assertEquals("James", map["name"])
    }
}
