/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections.js

import kotlin.test.*
import test.collections.*
import test.collections.behaviors.*

class ComplexMapJsTest : MapJsTest() {
    // Helper function with generic parameter to force to use ComlpexHashMap
    fun <K : kotlin.Comparable<K>> doTest() {
        HashMap<K, Int>()
        HashMap<K, Int>(3)
        HashMap<K, Int>(3, 0.5f)
        @Suppress("UNCHECKED_CAST")
        val map = HashMap<K, Int>(createTestMap() as HashMap<K, Int>)

        assertEquals(KEYS.toNormalizedList(), map.keys.toNormalizedList() as List<Any>)
        assertEquals(VALUES.toNormalizedList(), map.values.toNormalizedList())
    }

    @Test override fun constructors() {
        doTest<String>()
    }

    override fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T> = this.sorted()
    // hashMapOf returns ComlpexHashMap because it is Generic
    override fun emptyMutableMap(): MutableMap<String, Int> = genericHashMapOf()

    override fun emptyMutableMapWithNullableKeyValue(): MutableMap<String?, Int?> = genericHashMapOf()
}

class PrimitiveMapJsTest : MapJsTest() {
    @Test override fun constructors() {
        HashMap<String, Int>()
        HashMap<String, Int>(3)
        HashMap<String, Int>(3, 0.5f)

        val map = HashMap<String, Int>(createTestMap())

        assertEquals(KEYS.toNormalizedList(), map.keys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), map.values.toNormalizedList())
    }

    override fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T> = this.sorted()
    override fun emptyMutableMap(): MutableMap<String, Int> = stringMapOf()
    override fun emptyMutableMapWithNullableKeyValue(): MutableMap<String?, Int?> = HashMap()

    @Test fun compareBehavior() {
        val specialJsStringMap = stringMapOf<Any>()
        specialJsStringMap.put("k1", "v1")
        compare(genericHashMapOf("k1" to "v1"), specialJsStringMap) { mapBehavior() }

        val specialJsNumberMap = HashMap<Int, Any>(4)
        specialJsNumberMap.put(5, "v5")
        compare(genericHashMapOf(5 to "v5"), specialJsNumberMap) { mapBehavior() }
    }

    @Test fun putNull() {
        val map = stringMapOf("k" to null)
        assertEquals(1, map.size)

        map.put("k", null)
        assertEquals(1, map.size)

        map["k"] = null
        assertEquals(1, map.size)

        map.remove("k")
        assertEquals(0, map.size)
    }
}

class LinkedHashMapJsTest : LinkedMapJsTest() {
    @Test override fun constructors() {
        LinkedHashMap<String, Int>()
        LinkedHashMap<String, Int>(3)
        LinkedHashMap<String, Int>(3, 0.5f)

        val map = LinkedHashMap<String, Int>(createTestMap())

        assertEquals(KEYS.toNormalizedList(), map.keys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), map.values.toNormalizedList())
    }

    override fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T> = this.toList()
    override fun emptyMutableMap(): MutableMap<String, Int> = LinkedHashMap()
    override fun emptyMutableMapWithNullableKeyValue(): MutableMap<String?, Int?> = LinkedHashMap()
}

class LinkedPrimitiveMapJsTest : LinkedMapJsTest() {
    @Test override fun constructors() {
        val map = createTestMap()

        assertEquals(KEYS.toNormalizedList(), map.keys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), map.values.toNormalizedList())
    }

    override fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T> = this.toList()
    override fun emptyMutableMap(): MutableMap<String, Int> = linkedStringMapOf()
    override fun emptyMutableMapWithNullableKeyValue(): MutableMap<String?, Int?> = LinkedHashMap()
}

abstract class MapJsTest {
    val KEYS = listOf("zero", "one", "two", "three")
    val VALUES = arrayOf(0, 1, 2, 3).toList()

    val SPECIAL_NAMES = arrayOf(
        "__proto__",
        "constructor",
        "toString",
        "toLocaleString",
        "valueOf",
        "hasOwnProperty",
        "isPrototypeOf",
        "propertyIsEnumerable"
    )

    @Test fun getOrElse() {
        val data = emptyMap()
        val a = data.getOrElse("foo") { 2 }
        assertEquals(2, a)

        val b = data.getOrElse("foo") { 3 }
        assertEquals(3, b)
        assertEquals(0, data.size)
    }

    @Test fun getOrPut() {
        val data = emptyMutableMap()
        val a = data.getOrPut("foo") { 2 }
        assertEquals(2, a)

        val b = data.getOrPut("foo") { 3 }
        assertEquals(2, b)

        assertEquals(1, data.size)
    }

    @Test fun emptyMapGet() {
        val map = emptyMap()
        assertEquals(null, map.get("foo"), """failed on map.get("foo")""")
        assertEquals(null, map["bar"], """failed on map["bar"]""")
    }

    @Test fun mapGet() {
        val map = createTestMap()
        for (i in KEYS.indices) {
            assertEquals(VALUES[i], map.get(KEYS[i]), """failed on map.get(KEYS[$i])""")
            assertEquals(VALUES[i], map[KEYS[i]], """failed on map[KEYS[$i]]""")
        }

        assertEquals(null, map.get("foo"))
    }

    @Test fun mapPut() {
        val map = emptyMutableMap()

        map.put("foo", 1)
        assertEquals(1, map["foo"])
        assertEquals(null, map["bar"])

        map["bar"] = 2
        assertEquals(1, map["foo"])
        assertEquals(2, map["bar"])

        map["foo"] = 0
        assertEquals(0, map["foo"])
        assertEquals(2, map["bar"])
    }

    @Test fun sizeAndEmptyForEmptyMap() {
        val data = emptyMap()

        assertTrue(data.isEmpty())
        assertTrue(data.none())

        assertEquals(0, data.size)
        assertEquals(0, data.size)
    }

    @Test fun sizeAndEmpty() {
        val data = createTestMap()

        assertFalse(data.isEmpty())
        assertFalse(data.none())

        assertEquals(KEYS.size, data.size)
    }

    // #KT-3035
    @Test fun emptyMapValues() {
        val emptyMap = emptyMap()
        assertTrue(emptyMap.values.isEmpty())
    }

    @Test fun mapValues() {
        val map = createTestMap()
        assertEquals(VALUES.toNormalizedList(), map.values.toNormalizedList())
    }

    @Test fun mapKeySet() {
        val map = createTestMap()
        assertEquals(KEYS.toNormalizedList(), map.keys.toNormalizedList())
    }

    @Test fun mapEntrySet() {
        val map = createTestMap()

        val actualKeys = ArrayList<String>()
        val actualValues = ArrayList<Int>()
        for (e in map.entries) {
            actualKeys.add(e.key)
            actualValues.add(e.value)
        }

        assertEquals(KEYS.toNormalizedList(), actualKeys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), actualValues.toNormalizedList())
    }

    @Test fun mapContainsKey() {
        val map = createTestMap()

        assertTrue(map.containsKey(KEYS[0]) &&
                   map.containsKey(KEYS[1]) &&
                   map.containsKey(KEYS[2]) &&
                   map.containsKey(KEYS[3]))

        assertFalse(map.containsKey("foo") ||
                    map.containsKey(1 as Any))
    }

    @Test fun mapContainsValue() {
        val map = createTestMap()

        assertTrue(map.containsValue(VALUES[0]) &&
                   map.containsValue(VALUES[1]) &&
                   map.containsValue(VALUES[2]) &&
                   map.containsValue(VALUES[3]))

        assertFalse(map.containsValue("four" as Any) ||
                    map.containsValue(5))
    }

    @Test fun mapPutAll() {
        val map = createTestMap()
        val newMap = emptyMutableMap()
        newMap.putAll(map)
        assertEquals(KEYS.size, newMap.size)
    }

    @Test fun mapPutAllFromCustomMap() {
        val newMap = emptyMutableMap()
        newMap.putAll(ConstMap)
        assertEquals(ConstMap.entries.single().toPair(), newMap.entries.single().toPair())
    }

    @Test fun mapRemove() {
        val map = createTestMutableMap()
        val last = KEYS.size - 1
        val first = 0
        val mid = KEYS.size / 2

        assertEquals(KEYS.size, map.size)

        assertEquals(null, map.remove("foo"))
        assertEquals(VALUES[mid], map.remove(KEYS[mid]))
        assertEquals(null, map.remove(KEYS[mid]))
        assertEquals(VALUES[last], map.remove(KEYS[last]))
        assertEquals(VALUES[first], map.remove(KEYS[first]))

        assertEquals(KEYS.size - 3, map.size)
    }

    @Test fun mapClear() {
        val map = createTestMutableMap()
        assertFalse(map.isEmpty())
        map.clear()
        assertTrue(map.isEmpty())
    }

    @Test fun nullAsKey() {
        val map = emptyMutableMapWithNullableKeyValue()

        assertTrue(map.isEmpty())
        map.put(null, 23)
        assertFalse(map.isEmpty())
        assertTrue(map.containsKey(null))
        assertEquals(23, map[null])
        assertEquals(23, map.remove(null))
        assertTrue(map.isEmpty())
        assertEquals(null, map[null])
    }

    @Test fun nullAsValue() {
        val map = emptyMutableMapWithNullableKeyValue()
        val KEY = "Key"

        assertTrue(map.isEmpty())
        map.put(KEY, null)
        assertFalse(map.isEmpty())
        assertEquals(null, map[KEY])
        assertTrue(map.containsValue(null))
        assertEquals(null, map.remove(KEY))
        assertTrue(map.isEmpty())
    }

    @Test fun setViaIndexOperators() {
        val map = HashMap<String, String>()
        assertTrue{ map.isEmpty() }
        assertEquals(map.size, 0)

        map["name"] = "James"

        assertTrue{ !map.isEmpty() }
        assertEquals(map.size, 1)
        assertEquals("James", map["name"])
    }

    @Test fun createUsingPairs() {
        val map = mapOf(Pair("a", 1), Pair("b", 2))
        assertEquals(2, map.size)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
    }

    @Test fun createUsingTo() {
        val map = mapOf("a" to 1, "b" to 2)
        assertEquals(2, map.size)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
    }

    @Test fun mapIteratorImplicitly() {
        val map = createTestMap()

        val actualKeys = ArrayList<String>()
        val actualValues = ArrayList<Int>()
        for (e in map) {
            actualKeys.add(e.key)
            actualValues.add(e.value)
        }

        assertEquals(KEYS.toNormalizedList(), actualKeys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), actualValues.toNormalizedList())
    }

    @Test fun mapIteratorExplicitly() {
        val map = createTestMap()

        val actualKeys = ArrayList<String>()
        val actualValues = ArrayList<Int>()
        val iterator = map.iterator()
        for (e in iterator) {
            actualKeys.add(e.key)
            actualValues.add(e.value)
        }

        assertEquals(KEYS.toNormalizedList(), actualKeys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), actualValues.toNormalizedList())
    }

    @Test fun mapMutableIterator() {
        val map = createTestMutableMap()
        map.keys.removeAll { it == KEYS[0] }
        map.entries.removeAll { it.key == KEYS[1] }
        map.values.removeAll { it == VALUES[3] }

        assertEquals(1, map.size, "Expected 1 entry to remain in map, but got: $map")
    }

    @Test fun mapCollectionPropertiesAreViews() {
        val map = createTestMutableMap()
        assertTrue(map.size >= 3)
        val keys = map.keys
        val values = map.values
        val entries = map.entries

        val (key, value) = map.entries.first()

        map.remove(key)
        assertFalse(key in keys, "remove from map")
        assertFalse(value in values)
        assertFalse(entries.any { it.key == key })

        map.put(key, value)
        assertTrue(key in keys, "put to map")
        assertTrue(value in values)
        assertTrue(entries.any { it.key == key })

        keys -= key
        assertFalse(key in map, "remove from keys")
        assertFalse(value in values)
        assertFalse(entries.any { it.key == key })

        val (key2, value2) = map.entries.first()
        values -= value2
        assertFalse(key2 in map, "remove from values")
        assertFalse(map.containsValue(value2))
        assertFalse(entries.any { it.value == value2 })

        val entry = map.entries.first()
        val (key3, value3) = entry
        entries -= entry
        assertFalse(key3 in map, "remove from entries")
        assertFalse(key3 in keys)
        assertFalse(value3 in values)

        val entry2 = map.entries.first()
        entry2.setValue(100)
        assertEquals(100, map[entry2.key], "set value via entry")
    }

    @Test fun mapCollectionPropertiesDoNotSupportAdd() {
        val map = createTestMutableMap()
        val entry = map.entries.first()
        val (key, value) = entry

        assertFailsWith<UnsupportedOperationException> { map.entries += entry }
        assertFailsWith<UnsupportedOperationException> { map.keys += key }
        assertFailsWith<UnsupportedOperationException> { map.values += value }
    }

    @Test fun specialNamesNotContainsInEmptyMap() {
        val map = emptyMap()

        for (key in SPECIAL_NAMES) {
            assertFalse(map.containsKey(key), "unexpected key: $key")
        }
    }

    @Test fun specialNamesNotContainsInNonEmptyMap() {
        val map = createTestMap()

        for (key in SPECIAL_NAMES) {
            assertFalse(map.containsKey(key), "unexpected key: $key")
        }
    }

    @Test fun putAndGetSpecialNamesToMap() {
        val map = createTestMutableMap()
        var value = 0

        for (key in SPECIAL_NAMES) {
            assertFalse(map.containsKey(key), "unexpected key: $key")

            map.put(key, value)
            assertTrue(map.containsKey(key), "key not found: $key")

            val actualValue = map.get(key)
            assertEquals(value, actualValue, "wrong value fo key: $key")

            map.remove(key)
            assertFalse(map.containsKey(key), "unexpected key after remove: $key")

            value += 3
        }
    }

    @Test abstract fun constructors()

    private object ConstMap : Map<String, Int> {
        override val entries: Set<Map.Entry<String, Int>>
            get() = setOf(object : Map.Entry<String, Int> {
                override val key: String get() = "key"
                override val value: Int get() = 42
            })
        override val keys: Set<String> get() = setOf("key")
        override val size: Int get() = 1
        override val values = listOf(42)
        override fun containsKey(key: String) = key == "key"
        override fun containsValue(value: Int) = value == 42
        override fun get(key: String) = if (key == "key") 42 else null
        override fun isEmpty() = false
    }

    // Helpers

    abstract fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T>

    fun emptyMap(): Map<String, Int> = emptyMutableMap()

    abstract fun emptyMutableMap(): MutableMap<String, Int>

    abstract fun emptyMutableMapWithNullableKeyValue(): MutableMap<String?, Int?>

    fun createTestMap(): Map<String, Int> = createTestMutableMap()

    fun createTestMutableMap(): MutableMap<String, Int> {
        val map = emptyMutableMap()
        for (i in KEYS.indices) {
            map.put(KEYS[i], VALUES[i])
        }
        return map
    }

    fun <K, V> genericHashMapOf(vararg values: Pair<K, V>) = hashMapOf(*values)
}

abstract class LinkedMapJsTest : MapJsTest() {
    private fun <T> assertSameOrder(expected: Collection<T>, actual: Collection<T>, message: String) {
        assertEquals(expected.size, actual.size, "$message; different elements count")
        expected.zip(actual).forEachIndexed { index, (a, b) ->
            assertEquals(a, b, "$message; wrong element at index $index")
        }
    }

    private data class FakeEntry(override val key: String, override val value: Int) : Map.Entry<String, Int> {
        override fun equals(other: Any?): Boolean {
            return other is Map.Entry<*, *> && other.key == key && other.value == value
        }

        override fun hashCode(): Int {
            return 31 * key.hashCode() + value
        }
    }

    @Test
    fun insertionOrder() {
        val map = createTestMap()
        assertSameOrder(KEYS, map.keys, "keys order")
        assertSameOrder(VALUES, map.values, "values order")
        assertSameOrder(KEYS.zip(VALUES, ::FakeEntry), map.entries, "entries order")
    }

    @Test
    fun insertionOrderAfterRemovingFirstElement() {
        val map = createTestMutableMap()

        for (i in KEYS.indices) {
            assertEquals(map.remove(KEYS[i]), VALUES[i], "remove $i element")

            assertSameOrder(KEYS.drop(i + 1), map.keys, "keys order after removing $i")
            assertSameOrder(VALUES.drop(i + 1), map.values, "values order after removing $i")
            assertSameOrder(KEYS.zip(VALUES, ::FakeEntry).drop(i + 1), map.entries, "values order after removing $i")
        }
    }

    @Test
    fun insertionOrderAfterRemovingLastElement() {
        val map = createTestMutableMap()

        for (i in KEYS.indices.reversed()) {
            assertEquals(map.remove(KEYS[i]), VALUES[i], "remove $i element")

            assertSameOrder(KEYS.dropLast(KEYS.size - i), map.keys, "keys order after removing $i")
            assertSameOrder(VALUES.dropLast(VALUES.size - i), map.values, "values order after removing $i")
            assertSameOrder(KEYS.zip(VALUES, ::FakeEntry).dropLast(KEYS.size - i), map.entries, "values order after removing $i")
        }
    }

    @Test
    fun insertionOrderAfterRemovingMidElement() {
        val map = createTestMutableMap()

        assertEquals(map.remove(KEYS[1]), VALUES[1], "remove element")

        assertSameOrder(KEYS.filter { it != KEYS[1] }, map.keys, "keys order after removing")
        assertSameOrder(VALUES.filter { it != VALUES[1] }, map.values, "values order after removing")
        assertSameOrder(KEYS.zip(VALUES, ::FakeEntry).filter { it.key != KEYS[1] }, map.entries, "values order after removing")
    }

    @Test
    fun insertionOrderAfterRemovingWithIterator() {
        val map = createTestMutableMap()

        var newKeys = KEYS
        var newValues = VALUES
        val iter = map.iterator()

        val toRemove = listOf(true, true, false, true)
        for ((i, remove) in toRemove.withIndex()) {
            assertEquals<Map.Entry<*, *>>(FakeEntry(KEYS[i], VALUES[i]), iter.next(), "element ${KEYS[i]}")
            if (remove) {
                iter.remove()
                newKeys = newKeys.filter { it != KEYS[i] }
                newValues = newValues.filter { it != VALUES[i] }
            }

            assertSameOrder(newKeys, map.keys, "keys order after removing")
            assertSameOrder(newValues, map.values, "values order after removing")
            assertSameOrder(newKeys.zip(newValues, ::FakeEntry), map.entries, "values order after removing")
        }
    }

    @Test
    fun insertionOrderAfterInserting() {
        val map = createTestMutableMap()

        map[KEYS[0]] = VALUES[0]
        map[KEYS[2]] = VALUES[2]

        assertSameOrder(KEYS, map.keys, "keys order")
        assertSameOrder(VALUES, map.values, "values order")
        assertSameOrder(KEYS.zip(VALUES, ::FakeEntry), map.entries, "entries order")

        map["extra key 1"] = 101
        map["extra key 2"] = 102

        val newKeys = KEYS + listOf("extra key 1", "extra key 2")
        val newValues = VALUES + listOf(101, 102)

        assertSameOrder(newKeys, map.keys, "keys order")
        assertSameOrder(newValues, map.values, "values order")
        assertSameOrder(newKeys.zip(newValues, ::FakeEntry), map.entries, "entries order")
    }

    @Test
    fun insertionOrderAfterInsertingRemoving() {
        val map = emptyMutableMap()

        var entries = (1 until 100).map { FakeEntry("key$it", it) }
        val insertingRemoving = listOf(
            -1, -99, 1, -10, 150, 99, -2, 10, -150, -49, 101, -99, -98, -97, -77, -1, 2,
            +102, 103, 1, -4, 104, 4, -3, -5, -6, -7, 7, 3, -101, 150, -8, -9, -10, 8,
            +200, 201, 202, 203, 204, 205, 206, -200, -201, -102, -11, -12, -13, -14, -15,
            136, -7, 117, -43, -70, 106, -74, -91, 133, 5, 6, -85, -58, 145, -54, -34, 74, -93,
            146, -38, -40, -16, 118, 114, -61, -81, -6, -88, -67, -39, -94, -28, -63, -86, 61,
            -21, -82, -33, 28, -84, -37, 115, -30, 112, 14, -73, -79, 116, 16, 148, 33, 113, 86,
            -68, -45, 100, -74, -56, 21, -96, 131, 9, -46, 137, -18, -87, 135, 97, -133, -32, 108,
            -22, 58, 68, 63, 45, -35, 99, -68, -114, -89, 134, 35, -29, 144, -25, 7, 96, -33, 82,
        )

        entries.forEach { map[it.key] = it.value }
        for (i in insertingRemoving) {
            if (i < 0) {
                entries = entries.filter { it.value != -i }
                assertEquals(map.remove("key${-i}"), -i, "remove element ${-i}")
            } else {
                entries = entries + listOf(FakeEntry("key$i", i))
                assertEquals(map.put("key$i", i), null, "insert element $i")
            }

            assertSameOrder(entries.map { it.key }, map.keys, "keys order after $i")
            assertSameOrder(entries.map { it.value }, map.values, "values order after $i")
            assertSameOrder(entries, map.entries, "values order after $i")
        }
    }
}
