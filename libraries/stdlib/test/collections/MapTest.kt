/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*
import test.*
import test.collections.js.linkedStringMapOf
import test.collections.js.stringMapOf
import kotlin.math.pow

class MapTest {

    @Test fun getOrElse() {
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

    @Test fun getValue() {
        val data: MutableMap<String, Int> = hashMapOf("bar" to 1)
        assertFailsWith<NoSuchElementException> { data.getValue("foo") }.let { e ->
            assertTrue("foo" in e.message!!)
        }
        assertEquals(1, data.getValue("bar"))

        val mutableWithDefault = data.withDefault { 42 }
        assertEquals(42, mutableWithDefault.getValue("foo"))

        // verify that it is wrapper
        mutableWithDefault["bar"] = 2
        assertEquals(2, data["bar"])
        data["bar"] = 3
        assertEquals(3, mutableWithDefault["bar"])

        val readonlyWithDefault = (data as Map<String, Int>).withDefault { it.length }
        assertEquals(4, readonlyWithDefault.getValue("loop"))

        val withReplacedDefault = readonlyWithDefault.withDefault { 42 }
        assertEquals(42, withReplacedDefault.getValue("loop"))
    }

    @Test fun getOrPut() {
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

    @Test fun sizeAndEmpty() {
        val data = hashMapOf<String, Int>()
        assertTrue { data.none() }
        assertEquals(data.size, 0)
    }

    @Test fun setViaIndexOperators() {
        val map = hashMapOf<String, String>()
        assertTrue { map.none() }
        assertEquals(map.size, 0)

        map["name"] = "James"

        assertTrue { map.any() }
        assertEquals(map.size, 1)
        assertEquals("James", map["name"])
    }

    @Test fun iterate() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for (e in map) {
            list.add(e.key)
            list.add(e.value)
        }

        assertEquals(6, list.size)
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
    }

    @Test fun iterateAndMutate() {
        val map = mutableMapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val it = map.iterator()
        for (e in it) {
            when (e.key) {
                "beverage" -> e.setValue("juice")
                "location" -> it.remove()
            }
        }
        assertEquals(mapOf("beverage" to "juice", "name" to "James"), map)
    }


    @Test
    fun onEach() {
        val map = mutableMapOf("beverage" to "beer", "location" to "Mells")
        val result = StringBuilder()
        val newMap = map.onEach { result.append(it.key).append("=").append(it.value).append(";") }
        assertEquals("beverage=beer;location=Mells;", result.toString())
        assertTrue(map === newMap)

        // static types test
        assertStaticTypeIs<HashMap<String, String>>(
                hashMapOf("a" to "b").onEach {  }
        )
    }

    @Test
    fun onEachIndexed() {
        val map = mutableMapOf("beverage" to "beer", "location" to "Mells")
        val result = StringBuilder()
        val newMap = map.onEachIndexed { i, e -> result.append(i + 1).append('.').append(e.key).append("=").append(e.value).append(";") }
        assertEquals("1.beverage=beer;2.location=Mells;", result.toString())
        assertTrue(map === newMap)

        // static types test
        assertStaticTypeIs<HashMap<String, String>>(
            hashMapOf("a" to "b").onEachIndexed { _, _ -> }
        )
    }

    @Test fun stream() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val named = map.asSequence().filter { it.key == "name" }.single()
        assertEquals("James", named.value)
    }

    @Test fun iterateWithProperties() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for (e in map) {
            list.add(e.key)
            list.add(e.value)
        }

        assertEquals(6, list.size)
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
    }

    @Test fun iterateWithExtraction() {
        val map = mapOf("beverage" to "beer", "location" to "Mells", "name" to "James")
        val list = arrayListOf<String>()
        for ((key, value) in map) {
            list.add(key)
            list.add(value)
        }

        assertEquals(6, list.size)
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
    }

    @Test fun contains() {
        val map = mapOf("a" to 1, "b" to 2)
        assertTrue("a" in map)
        assertTrue("c" !in map)
    }

    @Test fun map() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val list = m1.map { it.value + " rocks" }

        assertEquals(listOf("beer rocks", "Mells rocks"), list)
    }


    @Test fun mapNotNull() {
        val m1 = mapOf("a" to 1, "b" to null)
        val list = m1.mapNotNull { it.value?.let { v -> "${it.key}$v" } }
        assertEquals(listOf("a1"), list)
    }

    @Test fun mapValues() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val m2 = m1.mapValues { it.value + "2" }

        assertEquals(mapOf("beverage" to "beer2", "location" to "Mells2"), m2)

        val m1p: Map<out String, String> = m1
        val m3 = m1p.mapValuesTo(hashMapOf()) { it.value.length }
        assertStaticTypeIs<HashMap<String, Int>>(m3)
        assertEquals(mapOf("beverage" to 4, "location" to 5), m3)
    }

    @Test fun mapKeys() {
        val m1 = mapOf("beverage" to "beer", "location" to "Mells")
        val m2 = m1.mapKeys { it.key + "2" }

        assertEquals(mapOf("beverage2" to "beer", "location2" to "Mells"), m2)

        val m1p: Map<out String, String> = m1
        val m3 = m1p.mapKeysTo(mutableMapOf()) { it.key.length }
        assertStaticTypeIs<MutableMap<Int, String>>(m3)
        assertEquals(mapOf(8 to "Mells"), m3)
    }

    @Test fun flatMap() {
        fun <T> list(entry: Map.Entry<T, T>): List<T> = listOf(entry.key, entry.value)
        fun <T> seq(entry: Map.Entry<T, T>): Sequence<T> = sequenceOf(entry.key, entry.value)
        val m = mapOf("x" to 1, "y" to 0)
        val result1 = m.flatMap { list(it) }
        val result2 = m.flatMap { seq(it) }
        val result3 = m.flatMap(::list)
        val result4 = m.flatMap(::seq)
        val expected = listOf("x", 1, "y", 0)
        assertEquals(expected, result1)
        assertEquals(expected, result2)
        assertEquals(expected, result3)
        assertEquals(expected, result4)
    }

    @Test fun createFrom() {
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

    @Test fun populateTo() {
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

    @Test fun createWithSelector() {
        val map = listOf("a", "bb", "ccc").associateBy { it.length }
        assertEquals(3, map.size)
        assertEquals("a", map.get(1))
        assertEquals("bb", map.get(2))
        assertEquals("ccc", map.get(3))
    }

    @Test fun createWithSelectorAndOverwrite() {
        val map = listOf("aa", "bb", "ccc").associateBy { it.length }
        assertEquals(2, map.size)
        assertEquals("bb", map.get(2))
        assertEquals("ccc", map.get(3))
    }

    @Test fun createWithSelectorForKeyAndValue() {
        val map = listOf("a", "bb", "ccc").associateBy({ it.length }, { it.uppercase() })
        assertEquals(3, map.size)
        assertEquals("A", map[1])
        assertEquals("BB", map[2])
        assertEquals("CCC", map[3])
    }

    @Test fun createWithPairSelector() {
        val map = listOf("a", "bb", "ccc").associate { it.length to it.uppercase() }
        assertEquals(3, map.size)
        assertEquals("A", map[1])
        assertEquals("BB", map[2])
        assertEquals("CCC", map[3])
    }

    @Test fun createUsingTo() {
        val map = mapOf("a" to 1, "b" to 2)
        assertEquals(2, map.size)
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
    }

    @Test fun createMutableMap() {
        val map = mutableMapOf("b" to 1, "c" to 2)
        map.put("a", 3)
        assertEquals(listOf("b" to 1, "c" to 2, "a" to 3), map.toList())
    }

    @Test fun createLinkedMap() {
        val map = linkedMapOf(Pair("c", 3), Pair("b", 2), Pair("a", 1))
        assertEquals(1, map["a"])
        assertEquals(2, map["b"])
        assertEquals(3, map["c"])
        assertEquals(listOf("c", "b", "a"), map.keys.toList())
    }

    @Test fun filter() {
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

    @Test fun filterOutProjectedTo() {
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

    @Test fun any() {
        val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
        assertTrue(map.any())
        assertFalse(emptyMap<String, Int>().any())

        assertTrue(map.any { it.key == "b" })
        assertFalse(emptyMap<String, Int>().any { it.key == "b" })

        assertTrue(map.any { it.value == 2 })
        assertFalse(map.any { it.value == 5 })
    }

    @Test fun all() {
        val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
        assertTrue(map.all { it.key != "d" })
        assertTrue(emptyMap<String, Int>().all { it.key == "d" })

        assertTrue(map.all { it.value > 0 })
        assertFalse(map.all { it.value == 2 })
    }

    @Test fun countBy() {
        val map = mapOf(Pair("b", 3), Pair("c", 2), Pair("a", 2))
        assertEquals(3, map.count())

        val filteredByKey = map.count { it.key == "b" }
        assertEquals(1, filteredByKey)

        val filteredByValue = map.count { it.value == 2 }
        assertEquals(2, filteredByValue)
    }

    @Test fun filterNot() {
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

    @Test
    fun entriesCovariantContains() {
        // Based on https://youtrack.jetbrains.com/issue/KT-42428.
        fun doTest(implName: String, map: Map<String, Int>, key: String, value: Int) {
            class SimpleEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V> {
                override fun toString(): String = "$key=$value"
                override fun hashCode(): Int = key.hashCode() xor value.hashCode()
                override fun equals(other: Any?): Boolean =
                    other is Map.Entry<*, *> && key == other.key && value == other.value
            }

            val mapDescription = "$implName: ${map::class}"

            assertTrue(map.keys.contains(key), mapDescription)
            assertEquals(value, map[key], mapDescription)
            // This one requires special efforts to make it work this way.
            // map.entries can in fact be `MutableSet<MutableMap.MutableEntry>`,
            // which [contains] method takes [MutableEntry], so the compiler may generate special bridge
            // returning false for values that aren't [MutableEntry] (including [SimpleEntry]).
            assertTrue(map.entries.contains(SimpleEntry(key, value)), mapDescription)
            assertTrue(map.entries.toSet().contains(SimpleEntry(key, value)), "$mapDescription: reference")

            assertFalse(map.entries.contains(null as Any?), "$mapDescription: contains null")
            assertFalse(map.entries.contains("not an entry" as Any?), "$mapDescription: contains not an entry")
        }

        val mapLetterToIndex = ('a'..'z').mapIndexed { i, c -> "$c" to i }.toMap()
        doTest("default read-only", mapLetterToIndex, "h", 7)
        doTest("default mutable", mapLetterToIndex.toMutableMap(), "b", 1)
        doTest("HashMap", mapLetterToIndex.toMap(HashMap()), "c", 2)
        doTest("LinkedHashMap", mapLetterToIndex.toMap(LinkedHashMap()), "d", 3)

        val builtMap = buildMap {
            putAll(mapLetterToIndex)
            doTest("MapBuilder", this, "z", 25)
        }
        doTest("built Map", builtMap, "y", 24)

        doTest("stringMapOf", mapLetterToIndex.toMap(stringMapOf()), "x", 23)
        doTest("linkedStringMapOf", mapLetterToIndex.toMap(linkedStringMapOf()), "w", 22)
    }

    @Test
    fun entriesCovariantRemove() {
        fun doTest(implName: String, map: MutableMap<String, Int>, key: String, value: Int) {
            class SimpleEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V> {
                override fun toString(): String = "$key=$value"
                override fun hashCode(): Int = key.hashCode() xor value.hashCode()
                override fun equals(other: Any?): Boolean =
                    other is Map.Entry<*, *> && key == other.key && value == other.value
            }

            val mapDescription = "$implName: ${map::class}"

            assertTrue(map.entries.toMutableSet().remove(SimpleEntry(key, value) as Map.Entry<*, *>), "$mapDescription: reference")
            assertTrue(map.entries.remove(SimpleEntry(key, value) as Map.Entry<*, *>), mapDescription)

            assertFalse(map.entries.remove(null as Any?), "$mapDescription: remove null")
            assertFalse(map.entries.remove("not an entry" as Any?), "$mapDescription: remove not an entry")
        }

        val mapLetterToIndex = ('a'..'z').mapIndexed { i, c -> "$c" to i }.toMap()
        doTest("default mutable", mapLetterToIndex.toMutableMap(), "b", 1)
        doTest("HashMap", mapLetterToIndex.toMap(HashMap()), "c", 2)
        doTest("LinkedHashMap", mapLetterToIndex.toMap(LinkedHashMap()), "d", 3)

        buildMap {
            putAll(mapLetterToIndex)
            doTest("MapBuilder", this, "z", 25)
        }

        doTest("stringMapOf", mapLetterToIndex.toMap(stringMapOf()), "x", 23)
        doTest("linkedStringMapOf", mapLetterToIndex.toMap(linkedStringMapOf()), "w", 22)
    }

    @Test
    fun firstNotNullOf() {
        val map = mapOf("Alice" to 20, "Tom" to 13, "Bob" to 18)

        val firstAdult = map.firstNotNullOf { (name, age) -> name.takeIf { age >= 18 } }
        val firstAdultOrNull = map.firstNotNullOfOrNull { (name, age) -> name.takeIf { age >= 18 } }

        assertEquals("Alice", firstAdult)
        assertEquals("Alice", firstAdultOrNull)

        @Suppress("UNUSED_VARIABLE")
        assertFailsWith<NoSuchElementException> { val firstChild = map.firstNotNullOf { (name, age) -> name.takeIf { age <= 11 } } }
        val firstChildOrNull = map.firstNotNullOfOrNull { (name, age) -> name.takeIf { age <= 11 } }

        assertNull(firstChildOrNull)
    }


    fun testPlusAssign(doPlusAssign: (MutableMap<String, Int>) -> Unit) {
        val map = hashMapOf("a" to 1, "b" to 2)
        doPlusAssign(map)
        assertEquals(3, map.size)
        assertEquals(1, map["a"])
        assertEquals(4, map["b"])
        assertEquals(3, map["c"])
    }

    @Test fun plusAssign() = testPlusAssign {
        it += "b" to 4
        it += "c" to 3
    }

    @Test fun plusAssignList() = testPlusAssign { it += listOf("c" to 3, "b" to 4) }

    @Test fun plusAssignArray() = testPlusAssign { it += arrayOf("c" to 3, "b" to 4) }

    @Test fun plusAssignSequence() = testPlusAssign { it += sequenceOf("c" to 3, "b" to 4) }

    @Test fun plusAssignMap() = testPlusAssign { it += mapOf("c" to 3, "b" to 4) }

    fun testPlus(doPlus: (Map<String, Int>) -> Map<String, Int>) {
        val original = mapOf("A" to 1, "B" to 2)
        val extended = doPlus(original)
        assertEquals(3, extended.size)
        assertEquals(1, extended["A"])
        assertEquals(4, extended["B"])
        assertEquals(3, extended["C"])
    }

    @Test fun plus() = testPlus { it + ("C" to 3) + ("B" to 4) }

    @Test fun plusList() = testPlus { it + listOf("C" to 3, "B" to 4) }

    @Test fun plusArray() = testPlus { it + arrayOf("C" to 3, "B" to 4) }

    @Test fun plusSequence() = testPlus { it + sequenceOf("C" to 3, "B" to 4) }

    @Test fun plusMap() = testPlus { it + mapOf("C" to 3, "B" to 4) }

    @Test fun plusAny() {
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


    fun testMinus(doMinus: (Map<String, Int>) -> Map<String, Int>) {
        val original = mapOf("A" to 1, "B" to 2)
        val shortened = doMinus(original)
        assertEquals("A" to 1, shortened.entries.single().toPair())
    }

    @Test fun minus() = testMinus { it - "B" - "C" }

    @Test fun minusList() = testMinus { it - listOf("B", "C") }

    @Test fun minusArray() = testMinus { it - arrayOf("B", "C") }

    @Test fun minusSequence() = testMinus { it - sequenceOf("B", "C") }

    @Test fun minusSet() = testMinus { it - setOf("B", "C") }



    fun testMinusAssign(doMinusAssign: (MutableMap<String, Int>) -> Unit) {
        val original = hashMapOf("A" to 1, "B" to 2)
        doMinusAssign(original)
        assertEquals("A" to 1, original.entries.single().toPair())
    }

    @Test fun minusAssign() = testMinusAssign {
        it -= "B"
        it -= "C"
    }

    @Test fun minusAssignList() = testMinusAssign { it -= listOf("B", "C") }

    @Test fun minusAssignArray() = testMinusAssign { it -= arrayOf("B", "C") }

    @Test fun minusAssignSequence() = testMinusAssign { it -= sequenceOf("B", "C") }


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


    @Test fun plusEmptyList() = testIdempotent { it + listOf() }

    @Test fun plusEmptySet() = testIdempotent { it + setOf() }

    @Test fun plusAssignEmptyList() = testIdempotentAssign { it += listOf() }

    @Test fun plusAssignEmptySet() = testIdempotentAssign { it += setOf() }


    private fun <K, V> expectMinMaxWith(min: Pair<K, V>, max: Pair<K, V>, elements: Map<K, V>, comparator: Comparator<Map.Entry<K, V>>) {
        assertEquals(min, elements.minWithOrNull(comparator)?.toPair())
        assertEquals(max, elements.maxWithOrNull(comparator)?.toPair())
        assertEquals(min, elements.minWith(comparator).toPair())
        assertEquals(max, elements.maxWith(comparator).toPair())
    }

    @Test
    fun minMaxWith() {
        val map = listOf("a", "bcd", "Ef").associateWith { it.length }
        expectMinMaxWith("Ef" to 2, "bcd" to 3, map, compareBy { it.key })
        expectMinMaxWith("a" to 1, "Ef" to 2, map, compareBy(String.CASE_INSENSITIVE_ORDER) { it.key })
        expectMinMaxWith("a" to 1, "bcd" to 3, map, compareBy { it.value })

    }

    @Test
    fun minMaxWithEmpty() {
        val empty = mapOf<Int, Int>()
        val comparator = compareBy<Map.Entry<Int, Int>> { it.value }
        assertNull(empty.minWithOrNull(comparator))
        assertNull(empty.maxWithOrNull(comparator))
        assertFailsWith<NoSuchElementException> { empty.minWith(comparator) }
        assertFailsWith<NoSuchElementException> { empty.maxWith(comparator) }
    }


    private inline fun <K, V, R : Comparable<R>> expectMinMaxBy(min: Pair<K, V>, max: Pair<K, V>, elements: Map<K, V>, selector: (Map.Entry<K, V>) -> R) {
        assertEquals(min, elements.minBy(selector).toPair())
        assertEquals(min, elements.minByOrNull(selector)?.toPair())
        assertEquals(max, elements.maxBy(selector).toPair())
        assertEquals(max, elements.maxByOrNull(selector)?.toPair())
    }

    @Test
    fun minMaxBy() {
        val map = listOf("a", "bcd", "Ef").associateWith { it.length }
        expectMinMaxBy("Ef" to 2, "bcd" to 3, map, { it.key })
        expectMinMaxBy("a" to 1, "Ef" to 2, map, { it.key.lowercase() })
        expectMinMaxBy("a" to 1, "bcd" to 3, map, { it.value })
    }

    @Test
    fun minMaxByEmpty() {
        val empty = mapOf<Int, Int>()
        assertNull(empty.minByOrNull { it.toString() })
        assertNull(empty.maxByOrNull { it.toString() })
        assertFailsWith<NoSuchElementException> { empty.minBy { it.toString() } }
        assertFailsWith<NoSuchElementException> { empty.maxBy { it.toString() } }
    }

    @Test fun minBySelectorEvaluateOnce() {
        val source = listOf(1, 2, 3).associateWith { it }
        var c = 0
        source.minBy { c++ }
        assertEquals(3, c)
        c = 0
        source.minByOrNull { c++ }
        assertEquals(3, c)
    }

    @Test fun maxBySelectorEvaluateOnce() {
        val source = listOf(1, 2, 3).associateWith { it }
        var c = 0
        source.maxBy { c++ }
        assertEquals(3, c)
        c = 0
        source.maxByOrNull { c++ }
        assertEquals(3, c)
    }

    private inline fun <K, V, R : Comparable<R>> expectMinMaxOf(min: R, max: R, elements: Map<K, V>, selector: (Map.Entry<K, V>) -> R) {
        assertEquals(min, elements.minOf(selector))
        assertEquals(min, elements.minOfOrNull(selector))
        assertEquals(max, elements.maxOf(selector))
        assertEquals(max, elements.maxOfOrNull(selector))
    }

    @Test
    fun minMaxOf() {
        val maps = (1..3).map { size -> listOf("a", "bcd", "Ef").take(size).associateWith { it.length } }

        expectMinMaxOf("a=1", "a=1", maps[0], { it.toString() })
        expectMinMaxOf("a=1", "bcd=3", maps[1], { it.toString() })
        expectMinMaxOf("Ef=2", "bcd=3",  maps[2], { it.toString() })
    }

    @Test
    fun minMaxOfDouble() {
        val items = mapOf("a" to 0.0, "b" to 1.0, "c" to -1.0)
        assertTrue(items.minOf { it.value.pow(0.5) }.isNaN())
        assertTrue(items.minOfOrNull { it.value.pow(0.5) }!!.isNaN())
        assertTrue(items.maxOf { it.value.pow(0.5) }.isNaN())
        assertTrue(items.maxOfOrNull { it.value.pow(0.5) }!!.isNaN())

        assertIsNegativeZero(items.minOf { it.value * 0.0 })
        assertIsNegativeZero(items.minOfOrNull { it.value * 0.0 }!!)
        assertIsPositiveZero(items.maxOf { it.value * 0.0 })
        assertIsPositiveZero(items.maxOfOrNull { it.value * 0.0 }!!)
    }

    @Test
    fun minMaxOfFloat() {
        val items = mapOf("a" to 0.0F, "b" to 1.0F, "c" to -1.0F)
        assertTrue(items.minOf { it.value.pow(0.5F) }.isNaN())
        assertTrue(items.minOfOrNull { it.value.pow(0.5F) }!!.isNaN())
        assertTrue(items.maxOf { it.value.pow(0.5F) }.isNaN())
        assertTrue(items.maxOfOrNull { it.value.pow(0.5F) }!!.isNaN())

        assertIsNegativeZero(items.minOf { it.value * 0.0F }.toDouble())
        assertIsNegativeZero(items.minOfOrNull { it.value * 0.0F }!!.toDouble())
        assertIsPositiveZero(items.maxOf { it.value * 0.0F }.toDouble())
        assertIsPositiveZero(items.maxOfOrNull { it.value * 0.0F }!!.toDouble())
    }

    @Test
    fun minMaxOfEmpty() {
        val empty = mapOf<Int, Int>()

        assertNull(empty.minOfOrNull { it.toString() })
        assertNull(empty.maxOfOrNull { it.toString() })
        assertFailsWith<NoSuchElementException> { empty.minOf { it.toString() } }
        assertFailsWith<NoSuchElementException> { empty.maxOf { it.toString() } }


        assertNull(empty.minOfOrNull { 0.0 })
        assertNull(empty.maxOfOrNull { 0.0 })
        assertFailsWith<NoSuchElementException> { empty.minOf { 0.0 } }
        assertFailsWith<NoSuchElementException> { empty.maxOf { 0.0 } }


        assertNull(empty.minOfOrNull { 0.0F })
        assertNull(empty.maxOfOrNull { 0.0F })
        assertFailsWith<NoSuchElementException> { empty.minOf { 0.0F } }
        assertFailsWith<NoSuchElementException> { empty.maxOf { 0.0F } }
    }


    private inline fun <K, V, R> expectMinMaxOfWith(min: R, max: R, elements: Map<K, V>, comparator: Comparator<R>, selector: (Map.Entry<K, V>) -> R) {
        assertEquals(min, elements.minOfWith(comparator, selector))
        assertEquals(min, elements.minOfWithOrNull(comparator, selector))
        assertEquals(max, elements.maxOfWith(comparator, selector))
        assertEquals(max, elements.maxOfWithOrNull(comparator, selector))
    }

    @Test
    fun minMaxOfWith() {
        val maps = (1..3).map { size -> listOf("a", "bcd", "Ef").take(size).associateWith { it.length } }
        val comparator = String.CASE_INSENSITIVE_ORDER
        expectMinMaxOfWith("a=1", "a=1", maps[0], comparator, { it.toString() })
        expectMinMaxOfWith("a=1", "bcd=3", maps[1], comparator, { it.toString() })
        expectMinMaxOfWith("a=1", "Ef=2", maps[2], comparator, { it.toString() })
    }

    @Test
    fun minMaxOfWithEmpty() {
        val empty = mapOf<Int, Int>()
        assertNull(empty.minOfWithOrNull(naturalOrder()) { it.toString() })
        assertNull(empty.maxOfWithOrNull(naturalOrder()) { it.toString() })
        assertFailsWith<NoSuchElementException> { empty.minOfWith(naturalOrder()) { it.toString() } }
        assertFailsWith<NoSuchElementException> { empty.maxOfWith(naturalOrder()) { it.toString() } }
    }

    @Test
    fun constructorWithCapacity() {
        assertFailsWith<IllegalArgumentException> {
            HashMap<String, String>(/*initialCapacity = */-1)
        }
        assertFailsWith<IllegalArgumentException> {
            HashMap<String, String>(/*initialCapacity = */-1, /*loadFactor = */0.5f)
        }
        assertFailsWith<IllegalArgumentException> {
            HashMap<String, String>(/*initialCapacity = */10, /*loadFactor = */0.0f)
        }
        assertFailsWith<IllegalArgumentException> {
            HashMap<String, String>(/*initialCapacity = */10, /*loadFactor = */Float.NaN)
        }
        assertEquals(0, HashMap<String, String>(/*initialCapacity = */0).size)
        assertEquals(0, HashMap<String, String>(/*initialCapacity = */10).size)
        assertEquals(0, HashMap<String, String>(/*initialCapacity = */0, /*loadFactor = */0.5f).size)
        assertEquals(0, HashMap<String, String>(/*initialCapacity = */10, /*loadFactor = */1.5f).size)

        assertFailsWith<IllegalArgumentException> {
            LinkedHashMap<String, String>(/*initialCapacity = */-1)
        }
        assertFailsWith<IllegalArgumentException> {
            LinkedHashMap<String, String>(/*initialCapacity = */-1, /*loadFactor = */0.5f)
        }
        assertFailsWith<IllegalArgumentException> {
            LinkedHashMap<String, String>(/*initialCapacity = */10, /*loadFactor = */0.0f)
        }
        assertFailsWith<IllegalArgumentException> {
            LinkedHashMap<String, String>(/*initialCapacity = */10, /*loadFactor = */Float.NaN)
        }
        assertEquals(0, LinkedHashMap<String, String>(/*initialCapacity = */0).size)
        assertEquals(0, LinkedHashMap<String, String>(/*initialCapacity = */10).size)
        assertEquals(0, LinkedHashMap<String, String>(/*initialCapacity = */0, /*loadFactor = */0.5f).size)
        assertEquals(0, LinkedHashMap<String, String>(/*initialCapacity = */10, /*loadFactor = */1.5f).size)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun kClassAsMapKey() {
        class A
        class B

        val kclasses = mapOf(
            String::class to 1,
            Char::class to 2,
            CharArray::class to 3,
            Double::class to 4,
            DoubleArray::class to 5,
            Float::class to 6,
            FloatArray::class to 7,
            Long::class to 8,
            LongArray::class to 9,
            ULong::class to 10,
            ULongArray::class to 11,
            Int::class to 12,
            IntArray::class to 13,
            UInt::class to 14,
            UIntArray::class to 15,
            Short::class to 16,
            ShortArray::class to 17,
            UShort::class to 18,
            UShortArray::class to 19,
            Byte::class to 20,
            ByteArray::class to 21,
            UByte::class to 22,
            UByteArray::class to 23,
            Boolean::class to 24,
            BooleanArray::class to 25,
            Unit::class to 26,
            Nothing::class to 27,
            A::class to 28,
            B::class to 29
        )

        assertEquals(kclasses[String::class], 1)
        assertEquals(kclasses[Char::class], 2)
        assertEquals(kclasses[CharArray::class], 3)
        assertEquals(kclasses[Double::class], 4)
        assertEquals(kclasses[DoubleArray::class], 5)
        assertEquals(kclasses[Float::class], 6)
        assertEquals(kclasses[FloatArray::class], 7)
        assertEquals(kclasses[Long::class], 8)
        assertEquals(kclasses[LongArray::class], 9)
        assertEquals(kclasses[ULong::class], 10)
        assertEquals(kclasses[ULongArray::class], 11)
        assertEquals(kclasses[Int::class], 12)
        assertEquals(kclasses[IntArray::class], 13)
        assertEquals(kclasses[UInt::class], 14)
        assertEquals(kclasses[UIntArray::class], 15)
        assertEquals(kclasses[Short::class], 16)
        assertEquals(kclasses[ShortArray::class], 17)
        assertEquals(kclasses[UShort::class], 18)
        assertEquals(kclasses[UShortArray::class], 19)
        assertEquals(kclasses[Byte::class], 20)
        assertEquals(kclasses[ByteArray::class], 21)
        assertEquals(kclasses[UByte::class], 22)
        assertEquals(kclasses[UByteArray::class], 23)
        assertEquals(kclasses[Boolean::class], 24)
        assertEquals(kclasses[BooleanArray::class], 25)
        assertEquals(kclasses[Unit::class], 26)
        assertEquals(kclasses[Nothing::class], 27)
        assertEquals(kclasses[A::class], 28)
        assertEquals(kclasses[B::class], 29)
    }
}
