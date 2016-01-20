package test.collections

import java.util.*
import kotlin.test.*
import org.junit.Test as test

class MapTest {

    @test fun getOrElse() {
        val data = mapOf<String, Int>()
        val a = data.getOrElse("foo") { 2 }
        assertEquals(2, a)
        val a1 = data.getOrElse("foo") { data.get("bar") } ?: 1
        assertEquals(1, a1)

        val b = data.getOrElse("foo") { 3 }
        assertEquals(3, b)
        assertEquals(0, data.size)

        val empty = mapOf<String, Int?>()
        val c = empty.getOrElse("") { null }
        assertEquals(null, c)
    }

    @test fun getOrImplicitDefault() {
        val data: MutableMap<String, Int> = hashMapOf("bar" to 1)
        assertTrue(assertFails { data.getOrImplicitDefault("foo") } is NoSuchElementException)
        assertEquals(1, data.getOrImplicitDefault("bar"))

        val mutableWithDefault = data.withDefault { 42 }
        assertEquals(42, mutableWithDefault.getOrImplicitDefault("foo"))

        // verify that it is wrapper
        mutableWithDefault["bar"] = 2
        assertEquals(2, data["bar"])
        data["bar"] = 3
        assertEquals(3, mutableWithDefault["bar"])

        val readonlyWithDefault = (data as Map<String, Int>).withDefault { it.length }
        assertEquals(4, readonlyWithDefault.getOrImplicitDefault("loop"))

        val withReplacedDefault = readonlyWithDefault.withDefault { 42 }
        assertEquals(42, withReplacedDefault.getOrImplicitDefault("loop"))
    }

    @test fun getOrPut() {
        val data = hashMapOf<String, Int>()
        val a = data.getOrPut("foo") { 2 }
        assertEquals(2, a)

        val b = data.getOrPut("foo") { 3 }
        assertEquals(2, b)

        assertEquals(1, data.size)

        val empty = hashMapOf<String, Int?>()
        val c = empty.getOrPut("") { null }
        assertEquals(null, c)

        val d = empty.getOrPut("") { 1 }
        assertEquals(null, d)  // soon will change to 1
    }

    @test fun sizeAndEmpty() {
        val data = hashMapOf<String, Int>()
        assertTrue { data.none() }
        assertEquals(data.size, 0)
    }

    @test fun setViaIndexOperators() {
        val map = hashMapOf<String, String>()
        assertTrue { map.none() }
        assertEquals(map.size, 0)

        map["name"] = "James"

        assertTrue { map.any() }
        assertEquals(map.size, 1)
        assertEquals("James", map["name"])
    }

    @test fun iterate() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for (e in map) {
            list.add(e.key)
            list.add(e.value)
        }

        assertEquals(6, list.size)
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
    }

    @test fun stream() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val named = map.asSequence().filter { it.key == "name" }.single()
        assertEquals("James", named.value)
    }

    @test fun iterateWithProperties() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for (e in map) {
            list.add(e.key)
            list.add(e.value)
        }

        assertEquals(6, list.size)
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
    }

    @test fun iterateWithExtraction() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for ((key, value) in map) {
            list.add(key)
            list.add(value)
        }

        assertEquals(6, list.size)
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
    }

    @test fun contains() {
        val map = mapOf("a" to 1, "b" to 2)
        assertTrue("a" in map)
        assertTrue("c" !in map)
    }

    @test fun map() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val list = m1.map { it.value + " rocks" }

        assertEquals(listOf("beer rocks", "Mells rocks"), list)
    }


    @test fun mapNotNull() {
        val m1 = mapOf("a" to 1, "b" to null)
        val list = m1.mapNotNull { it.value?.let { v -> "${it.key}$v" } }
        assertEquals(listOf("a1"), list)
    }

    @test fun mapValues() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val m2 = m1.mapValues { it.value + "2" }

        assertEquals("beer2", m2["beverage"])
        assertEquals("Mells2", m2["location"])
    }

    @test fun mapKeys() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val m2 = m1.mapKeys { it.key + "2" }

        assertEquals("beer", m2["beverage2"])
        assertEquals("Mells", m2["location2"])
    }

    @test fun createUsingPairs() {
        val map = mapOf(Pair("a", 1), Pair("b", 2))
        assertEquals(2, map.size)
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
    }

    @test fun createFromIterable() {
        val map = listOf(Pair("a", 1), Pair("b", 2)).toMap()
        assertEquals(2, map.size)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
    }

    @test fun createWithSelector() {
        val map = listOf("a", "bb", "ccc").associateBy { it.length }
        assertEquals(3, map.size)
        assertEquals("a", map.get(1))
        assertEquals("bb", map.get(2))
        assertEquals("ccc", map.get(3))
    }

    @test fun createWithSelectorAndOverwrite() {
        val map = listOf("aa", "bb", "ccc").associateBy { it.length }
        assertEquals(2, map.size)
        assertEquals("bb", map.get(2))
        assertEquals("ccc", map.get(3))
    }

    @test fun createWithSelectorForKeyAndValue() {
        val map = listOf("a", "bb", "ccc").associateBy({ it.length }, { it.toUpperCase() })
        assertEquals(3, map.size)
        assertEquals("A", map[1])
        assertEquals("BB", map[2])
        assertEquals("CCC", map[3])
    }

    @test fun createWithPairSelector() {
        val map = listOf("a", "bb", "ccc").associate { it.length to it.toUpperCase() }
        assertEquals(3, map.size)
        assertEquals("A", map[1])
        assertEquals("BB", map[2])
        assertEquals("CCC", map[3])
    }

    @test fun createUsingTo() {
        val map = mapOf("a" to 1, "b" to 2)
        assertEquals(2, map.size)
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
    }

    @test fun createMutableMap() {
        val map = mutableMapOf("b" to 1, "c" to 2)
        map.put("a", 3)
        assertEquals(listOf("b" to 1, "c" to 2, "a" to 3), map.toList())
    }

    @test fun createLinkedMap() {
        val map = linkedMapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(3, map["c"])
        assertEquals(listOf("c", "b", "a"), map.keys.toList())
    }

    @test fun filter() {
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

    @test fun any() {
        val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
        assertTrue(map.any())
        assertFalse(emptyMap<String, Int>().any())

        assertTrue(map.any { it.key == "b" })
        assertFalse(emptyMap<String, Int>().any { it.key == "b" })

        assertTrue(map.any { it.value == 2 })
        assertFalse(map.any { it.value == 5 })
    }

    @test fun all() {
        val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
        assertTrue(map.all { it.key != "d" })
        assertTrue(emptyMap<String, Int>().all { it.key == "d" })

        assertTrue(map.all { it.value > 0 })
        assertFalse(map.all { it.value == 2 })
    }

    @test fun countBy() {
        val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
        assertEquals(3, map.count())

        val filteredByKey = map.count { it.key == "b" }
        assertEquals(1, filteredByKey)

        val filteredByValue = map.count { it.value == 2 }
        assertEquals(2, filteredByValue)
    }

    @test fun filterNot() {
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

    fun testPlusAssign(doPlusAssign: (MutableMap<String, Int>) -> Unit) {
        val map = hashMapOf("a" to 1, "b" to 2)
        doPlusAssign(map)
        assertEquals(3, map.size)
        assertEquals(1, map["a"])
        assertEquals(4, map["b"])
        assertEquals(3, map["c"])
    }

    @test fun plusAssign() = testPlusAssign {
        it += "b" to 4
        it += "c" to 3
    }

    @test fun plusAssignList() = testPlusAssign { it += listOf("c" to 3, "b" to 4) }

    @test fun plusAssignArray() = testPlusAssign { it += arrayOf("c" to 3, "b" to 4) }

    @test fun plusAssignSequence() = testPlusAssign { it += sequenceOf("c" to 3, "b" to 4) }

    @test fun plusAssignMap() = testPlusAssign { it += mapOf("c" to 3, "b" to 4) }

    fun testPlus(doPlus: (Map<String, Int>) -> Map<String, Int>) {
        val original = mapOf("A" to 1, "B" to 2)
        val extended = doPlus(original)
        assertEquals(3, extended.size)
        assertEquals(1, extended["A"])
        assertEquals(4, extended["B"])
        assertEquals(3, extended["C"])
    }

    @test fun plus() = testPlus { it + ("C" to 3) + ("B" to 4) }

    @test fun plusList() = testPlus { it + listOf("C" to 3, "B" to 4) }

    @test fun plusArray() = testPlus { it + arrayOf("C" to 3, "B" to 4) }

    @test fun plusSequence() = testPlus { it + sequenceOf("C" to 3, "B" to 4) }

    @test fun plusMap() = testPlus { it + mapOf("C" to 3, "B" to 4) }


    fun testMinus(doMinus: (Map<String, Int>) -> Map<String, Int>) {
        val original = mapOf("A" to 1, "B" to 2)
        val shortened = doMinus(original)
        assertEquals("A" to 1, shortened.entries.single().toPair())
    }

    @test fun minus() = testMinus { it - "B" - "C" }

    @test fun minusList() = testMinus { it - listOf("B", "C") }

    @test fun minusArray() = testMinus { it - arrayOf("B", "C") }

    @test fun minusSequence() = testMinus { it - sequenceOf("B", "C") }

    @test fun minusSet() = testMinus { it - setOf("B", "C") }



    fun testMinusAssign(doMinusAssign: (MutableMap<String, Int>) -> Unit) {
        val original = hashMapOf("A" to 1, "B" to 2)
        doMinusAssign(original)
        assertEquals("A" to 1, original.entries.single().toPair())
    }

/*
    @test fun minusAssign() = testMinusAssign {
        it.keys -= "B"
        it.keys -= "C"
    }

    @test fun minusAssignList() = testMinusAssign { it.keys -= listOf("B", "C") }

    @test fun minusAssignArray() = testMinusAssign { it.keys -= arrayOf("B", "C") }

    @test fun minusAssignSequence() = testMinusAssign { it.keys -= sequenceOf("B", "C") }
*/


    fun testIdempotent(operation: (Map<String, Int>) -> Map<String, Int>) {
        val original = mapOf("A" to 1, "B" to 2)
        assertEquals(original, operation(original))
    }

    fun testIdempotentAssign(operation: (MutableMap<String, Int>) -> Unit) {
        val original = hashMapOf("A" to 1, "B" to 2)
        val result = HashMap(original)
        operation(result)
        assertEquals(original, result)
    }


    @test fun plusEmptyList() = testIdempotent { it + listOf() }
//    @test fun minusEmptyList() = testIdempotent { it - listOf() }

    @test fun plusEmptySet() = testIdempotent { it + setOf() }
//    @test fun minusEmptySet() = testIdempotent { it - setOf() }

    @test fun plusAssignEmptyList() = testIdempotentAssign { it += listOf() }
//    @test fun minusAssignEmptyList() = testIdempotentAssign { it -= listOf() }

    @test fun plusAssignEmptySet() = testIdempotentAssign { it += setOf() }
//    @test fun minusAssignEmptySet() = testIdempotentAssign { it -= setOf() }


}
