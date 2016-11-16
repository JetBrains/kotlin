package test.collections

import kotlin.test.*
import org.junit.Test as test

class MapTest {

    // just a static type check
    fun <T> assertStaticTypeIs(value: T) {}

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

        val nullable = mapOf(1 to null)
        val d = nullable.getOrElse(1) { "x" }
        assertEquals("x", d)
    }

    @Suppress("INVISIBLE_MEMBER")
    @test fun getOrImplicitDefault() {
        val data: MutableMap<String, Int> = hashMapOf("bar" to 1)
        assertFailsWith<NoSuchElementException> { data.getOrImplicitDefault("foo") }
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
        assertEquals(1, d)
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

        assertEquals(mapOf("beverage" to "beer2", "location" to "Mells2"), m2)

        val m1p: Map<out String, String> = m1
        val m3 = m1p.mapValuesTo(hashMapOf()) { it.value.length }
        assertStaticTypeIs<HashMap<String, Int>>(m3)
        assertEquals(mapOf("beverage" to 4, "location" to 5), m3)
    }

    @test fun mapKeys() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val m2 = m1.mapKeys { it.key + "2" }

        assertEquals(mapOf("beverage2" to "beer", "location2" to "Mells"), m2)

        val m1p: Map<out String, String> = m1
        val m3 = m1p.mapKeysTo(mutableMapOf()) { it.key.length }
        assertStaticTypeIs<MutableMap<Int, String>>(m3)
        assertEquals(mapOf(8 to "Mells"), m3)
    }

    @test fun createFrom() {
        val pairs = arrayOf("a" to 1, "b" to 2)
        val expected = mapOf(*pairs)

        assertEquals(expected, pairs.toMap())
        assertEquals(expected, pairs.asIterable().toMap())
        assertEquals(expected, pairs.asSequence().toMap())
        assertEquals(expected, expected.toMap())
        assertEquals(mapOf("a" to 1), expected.filterKeys { it == "a" }.toMap())
        assertEquals(emptyMap(), expected.filter { false }.toMap())

        val mutableMap = expected.toMutableMap()
        assertEquals(expected, mutableMap)
        mutableMap += "c" to 3
        assertNotEquals(expected, mutableMap)
    }

    @test fun populateTo() {
        val pairs = arrayOf("a" to 1, "b" to 2)
        val expected = mapOf(*pairs)

        val linkedMap: LinkedHashMap<String, Int> = pairs.toMap(linkedMapOf())
        assertEquals(expected, linkedMap)

        val hashMap: HashMap<String, Int> = pairs.asIterable().toMap(hashMapOf())
        assertEquals(expected, hashMap)

        val mutableMap: MutableMap<String, Int> = pairs.asSequence().toMap(mutableMapOf())
        assertEquals(expected, mutableMap)

        val mutableMap2 = mutableMap.toMap(mutableMapOf())
        assertEquals(expected, mutableMap2)

        val mutableMap3 = mutableMap.toMap(hashMapOf<CharSequence, Any>())
        assertEquals<Map<*, *>>(expected, mutableMap3)
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
        val filteredByKey = map.filter { it.key[0] == 'b' }
        assertEquals(mapOf("b" to 3), filteredByKey)

        val filteredByKey2 = map.filterKeys { it[0] == 'b' }
        assertEquals(mapOf("b" to 3), filteredByKey2)

        val filteredByValue = map.filter { it.value == 2 }
        assertEquals(mapOf("a" to 2, "c" to 2), filteredByValue)

        val filteredByValue2 = map.filterValues { it % 2 == 0 }
        assertEquals(mapOf("a" to 2, "c" to 2), filteredByValue2)
    }

    @test fun filterOutProjectedTo() {
        val map: Map<out String, Int> = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))

        val filteredByKey = map.filterTo(mutableMapOf()) { it.key[0] == 'b' }
        assertStaticTypeIs<MutableMap<String, Int>>(filteredByKey)
        assertEquals(mapOf("b" to 3), filteredByKey)

        val filteredByKey2 = map.filterKeys { it[0] == 'b' }
        assertStaticTypeIs<Map<String, Int>>(filteredByKey2)
        assertEquals(mapOf("b" to 3), filteredByKey2)

        val filteredByValue = map.filterNotTo(hashMapOf()) { it.value != 2 }
        assertStaticTypeIs<HashMap<String, Int>>(filteredByValue)
        assertEquals(mapOf("a" to 2, "c" to 2), filteredByValue)

        val filteredByValue2 = map.filterValues { it % 2 == 0 }
        assertStaticTypeIs<Map<String, Int>>(filteredByValue2)
        assertEquals(mapOf("a" to 2, "c" to 2), filteredByValue2)
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

    @test fun plusAny() {
        testPlusAny(emptyMap<String, String>(), 1 to "A")
        testPlusAny(mapOf("A" to null), "A" as CharSequence to 2)
    }

    fun <K, V> testPlusAny(mapObject: Any, pair: Pair<K, V>) {
        val map = mapObject as Map<*, *>
        fun assertContains(map: Map<*, *>) = assertEquals(pair.second, map[pair.first])

        assertContains(map + pair)
        assertContains(map + listOf(pair))
        assertContains(map + arrayOf(pair))
        assertContains(map + sequenceOf(pair))
        assertContains(map + mapOf(pair))
    }



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

    @test fun plusEmptySet() = testIdempotent { it + setOf() }

    @test fun plusAssignEmptyList() = testIdempotentAssign { it += listOf() }

    @test fun plusAssignEmptySet() = testIdempotentAssign { it += setOf() }


}
