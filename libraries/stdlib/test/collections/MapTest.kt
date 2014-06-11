package test.collections

import kotlin.test.*
import org.junit.Test as test

class MapTest {

    test fun getOrElse() {
        val data = mapOf<String, Int>()
        val a = data.getOrElse("foo") { 2 }
        assertEquals(2, a)

        val b = data.getOrElse("foo") { 3 }
        assertEquals(3, b)
        assertEquals(0, data.size())

        val empty = mapOf<String, Int?>()
        val c = empty.getOrElse("") { null }
        assertEquals(null, c)
    }

    test fun getOrPut() {
        val data = hashMapOf<String, Int>()
        val a = data.getOrPut("foo") { 2 }
        assertEquals(2, a)

        val b = data.getOrPut("foo") { 3 }
        assertEquals(2, b)

        assertEquals(1, data.size())

        val empty = hashMapOf<String, Int?>()
        val c = empty.getOrPut("") { null }
        assertEquals(null, c)
    }

    test fun sizeAndEmpty() {
        val data = hashMapOf<String, Int>()
        assertTrue { data.empty }
        assertEquals(data.size, 0)
    }

    test fun setViaIndexOperators() {
        val map = hashMapOf<String, String>()
        assertTrue { map.empty }
        assertEquals(map.size, 0)

        map["name"] = "James"

        assertTrue { !map.empty }
        assertEquals(map.size(), 1)
        assertEquals("James", map["name"])
    }
   
    test fun iterate() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for (e in map) {
            list.add(e.getKey())
            list.add(e.getValue())
        }

        assertEquals(6, list.size())
        assertEquals("beverage,beer,location,Mells,name,James", list.join(","))
    }

    test fun iterateWithProperties() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for (e in map) {
            list.add(e.key)
            list.add(e.value)
        }

        assertEquals(6, list.size())
        assertEquals("beverage,beer,location,Mells,name,James", list.join(","))
    }

    test fun iterateWithExtraction() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for ((key, value) in map) {
            list.add(key)
            list.add(value)
        }

        assertEquals(6, list.size())
        assertEquals("beverage,beer,location,Mells,name,James", list.join(","))
    }

    test fun contains() {
        val map = mapOf("a" to 1, "b" to 2)
        assert("a" in map)
        assert("c" !in map)
    }

    test fun map() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val list = m1.map { it.value + " rocks" }

        println("Got new list $list")
        assertEquals(arrayListOf("beer rocks", "Mells rocks"), list)
    }

    test fun mapValues() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val m2 = m1.mapValues { it.value + "2" }

        assertEquals("beer2", m2["beverage"])
        assertEquals("Mells2", m2["location"])
    }

    test fun mapKeys() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val m2 = m1.mapKeys { it.key + "2" }

        assertEquals("beer", m2["beverage2"])
        assertEquals("Mells", m2["location2"])
    }

    test fun createUsingPairs() {
        val map = mapOf(Pair("a", 1), Pair("b", 2))
        assertEquals(2, map.size)
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
    }

    test fun createFromIterable() {
        val map = listOf(Pair("a", 1), Pair("b", 2)).toMap()
        assertEquals(2, map.size)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
    }

    test fun createUsingTo() {
        val map = mapOf("a" to 1, "b" to 2)
        assertEquals(2, map.size)
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
    }

    test fun createLinkedMap() {
        val map = linkedMapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(3, map["c"])
        assertEquals(arrayListOf("c", "b", "a"), map.keySet().toList())
    }

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
