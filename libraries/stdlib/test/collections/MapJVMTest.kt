package test.collections

import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.expect
import org.junit.Test as test

class MapJVMTest {
    @test fun createSortedMap() {
        val map = sortedMapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(3, map["c"])
        assertEquals(listOf("a", "b", "c"), map.keySet().toList())
    }

    @test fun toSortedMap() {
        val map = mapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        val sorted = map.toSortedMap()
        assertEquals(1, sorted["a"])
        assertEquals(2, sorted["b"])
        assertEquals(3, sorted["c"])
        assertEquals(listOf("a", "b", "c"), sorted.keySet().toList())
    }

    @test fun toSortedMapWithComparator() {
        val map = mapOf(Pair("c", 3), Pair("bc", 2), Pair("bd", 4), Pair("abc", 1))
        val sorted = map.toSortedMap(compareBy<String> { it.length() } thenBy { it })
        assertEquals(listOf("c", "bc", "bd", "abc"), sorted.keySet().toList())
        assertEquals(1, sorted["abc"])
        assertEquals(2, sorted["bc"])
        assertEquals(3, sorted["c"])
    }

    @test fun toProperties() {
        val map = mapOf("a" to "A", "b" to "B")
        val prop = map.toProperties()
        assertEquals(2, prop.size())
        assertEquals("A", prop.getProperty("a", "fail"))
        assertEquals("B", prop.getProperty("b", "fail"))
    }

    @test fun getOrPutFailsOnConcurrentMap() {
        val map = ConcurrentHashMap<String, Int>()

        // now this is an error
        // map.getOrPut("x") { 1 }
        expect(1) {
            map.concurrentGetOrPut("x") { 1 }
        }
        expect(1) {
            (map as MutableMap<String, Int>).getOrPut("x") { 1 }
        }
    }
}
