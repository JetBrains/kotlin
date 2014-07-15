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
        assertTrue("a" in map)
        assertTrue("c" !in map)
    }

    test fun map() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val list = m1.map { it.value + " rocks" }

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

    test fun createWithSelector() {
        val map = listOf("a", "bb", "ccc").toMap { it.length }
        assertEquals(3, map.size)
        assertEquals("a", map.get(1))
        assertEquals("bb", map.get(2))
        assertEquals("ccc", map.get(3))
    }

    test fun createWithSelectorAndOverwrite() {
        val map = listOf("aa", "bb", "ccc").toMap { it.length }
        assertEquals(2, map.size)
        assertEquals("bb", map.get(2))
        assertEquals("ccc", map.get(3))
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

    test fun filter() {
        val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
        val filteredByKey = map.filter { it.key == "b" }
        assertEquals(1, filteredByKey.size)
        assertEquals(3, filteredByKey["b"])

        val filteredByKey2 = map.filterKeys { it == "b" }
        assertEquals(1, filteredByKey2.size)
        assertEquals(3, filteredByKey2["b"])

        val filteredByValue = map.filter { it.value == 2 }
        assertEquals(2, filteredByValue.size)
        assertEquals(null, filteredByValue["b"])
        assertEquals(2, filteredByValue["c"])
        assertEquals(2, filteredByValue["a"])

        val filteredByValue2 = map.filterValues { it == 2 }
        assertEquals(2, filteredByValue2.size)
        assertEquals(null, filteredByValue2["b"])
        assertEquals(2, filteredByValue2["c"])
        assertEquals(2, filteredByValue2["a"])
    }

    test fun filterNot() {
        val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
        val filteredByKey = map.filterNot { it.key == "b" }
        assertEquals(2, filteredByKey.size)
        assertEquals(null, filteredByKey["b"])
        assertEquals(2, filteredByKey["c"])
        assertEquals(2, filteredByKey["a"])

        val filteredByValue = map.filterNot { it.value == 2 }
        assertEquals(1, filteredByValue.size)
        assertEquals(3, filteredByValue["b"])
    }

    test fun plusAssign() {
        val extended = hashMapOf(Pair("b", 3))
        extended += ("c" to 2)
        assertEquals(2, extended.size)
        assertEquals(2, extended["c"])
        assertEquals(3, extended["b"])
    }
}
