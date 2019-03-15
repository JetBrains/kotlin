/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.collections

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.test.*

class MapJVMTest {
    @Test fun createSortedMap() {
        val map = sortedMapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(3, map["c"])
        assertEquals(listOf("a", "b", "c"), map.keys.toList())
    }

    @Test fun toSortedMap() {
        val map = mapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        val sorted = map.toSortedMap()
        assertEquals(1, sorted["a"])
        assertEquals(2, sorted["b"])
        assertEquals(3, sorted["c"])
        assertEquals(listOf("a", "b", "c"), sorted.keys.toList())
    }

    @Test fun toSortedMapWithComparator() {
        val map = mapOf(Pair("c", 3), Pair("bc", 2), Pair("bd", 4), Pair("abc", 1))
        val sorted = map.toSortedMap(compareBy<String> { it.length }.thenBy { it })
        assertEquals(listOf("c", "bc", "bd", "abc"), sorted.keys.toList())
        assertEquals(1, sorted["abc"])
        assertEquals(2, sorted["bc"])
        assertEquals(3, sorted["c"])
    }

    @Test fun toProperties() {
        val map = mapOf("a" to "A", "b" to "B")
        val prop = map.toProperties()
        assertEquals(2, prop.size)
        assertEquals("A", prop.getProperty("a", "fail"))
        assertEquals("B", prop.getProperty("b", "fail"))
    }

    @Test fun iterateAndRemove() {
        val map = (1..5).associateByTo(linkedMapOf(), { it }, { 'a' + it })
        val iterator = map.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().key % 2 == 0)
                iterator.remove()
        }
        assertEquals(listOf(1, 3, 5), map.keys.toList())
        assertEquals(listOf('b', 'd', 'f'), map.values.toList())
    }
    
    @Test fun getOrPutFailsOnConcurrentMap() {
        val map = ConcurrentHashMap<String, Int>()

        // not an error anymore
        expect(1) {
            map.getOrPut("x") { 1 }
        }
        expect(1) {
            (map as MutableMap<String, Int>).getOrPut("x") { 1 }
        }
    }
}
