package test.collections

import kotlin.test.*
import org.junit.Test as test

class MapJVMTest {
    test fun createSortedMap() {
        val map = sortedMapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(3, map["c"])
        assertEquals(arrayListOf("a", "b", "c"), map.keySet().toList())
    }

    test fun toSortedMap() {
        val map = mapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        val sorted = map.toSortedMap()
        assertEquals(1, sorted["a"])
        assertEquals(2, sorted["b"])
        assertEquals(3, sorted["c"])
        assertEquals(arrayListOf("a", "b", "c"), sorted.keySet().toList())
    }

    test fun toSortedMapWithComparator() {
        val map = mapOf(Pair("c", 3), Pair("bc", 2), Pair("bd", 4), Pair("abc", 1))
        val c = comparator<String>{ a, b ->
            val answer = a.length() - b.length()
            if (answer == 0) a.compareTo(b) else answer
        }
        val sorted = map.toSortedMap(c)
        assertEquals(arrayListOf("c", "bc", "bd", "abc"), sorted.keySet().toList())
        assertEquals(1, sorted["abc"])
        assertEquals(2, sorted["bc"])
        assertEquals(3, sorted["c"])
    }

    test fun toProperties() {
        val map = mapOf("a" to "A", "b" to "B")
        val prop = map.toProperties()
        assertEquals(2, prop.size)
        assertEquals("A", prop.getProperty("a", "fail"))
        assertEquals("B", prop.getProperty("b", "fail"))
    }
}
