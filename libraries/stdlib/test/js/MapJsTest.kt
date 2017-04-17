package test.collections.js

import kotlin.test.*
import org.junit.Test
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

class LinkedHashMapJsTest : MapJsTest() {
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

class LinkedPrimitiveMapJsTest : MapJsTest() {
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

    val SPECIAL_NAMES = arrayOf("__proto__", "constructor", "toString", "toLocaleString", "valueOf", "hasOwnProperty", "isPrototypeOf", "propertyIsEnumerable")

    @Test fun getOrElse() {
        val data = emptyMap()
        val a = data.getOrElse("foo"){2}
        assertEquals(2, a)

        val b = data.getOrElse("foo"){3}
        assertEquals(3, b)
        assertEquals(0, data.size)
    }

    @Test fun getOrPut() {
        val data = emptyMutableMap()
        val a = data.getOrPut("foo"){2}
        assertEquals(2, a)

        val b = data.getOrPut("foo"){3}
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
        entries -= entry
        assertFalse(entry.key in map, "remove from entries")
        assertFalse(entry.key in keys)
        assertFalse(entry.value in values)

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

    /*
    test fun createLinkedMap() {
        val map = linkedMapOf("c" to 3, "b" to 2, "a" to 1)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
        assertEquals(3, map.get("c"))
        assertEquals(arrayList("c", "b", "a"), map.keySet().toList())
    }

    test fun iterate() {
        val map = TreeMap<String, String>()
        map["beverage"] = "beer"
        map["location"] = "Mells"
        map["name"] = "James"

        val list = arrayList<String>()
        for (e in map) {
            println("key = ${e.getKey()}, value = ${e.getValue()}")
            list.add(e.getKey())
            list.add(e.getValue())
        }

        assertEquals(6, list.size())
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
    }

    test fun iterateWithProperties() {
        val map = TreeMap<String, String>()
        map["beverage"] = "beer"
        map["location"] = "Mells"
        map["name"] = "James"

        val list = arrayList<String>()
        for (e in map) {
            println("key = ${e.key}, value = ${e.value}")
            list.add(e.key)
            list.add(e.value)
        }

        assertEquals(6, list.size())
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
    }

    test fun map() {
        val m1 = TreeMap<String, String>()
        m1["beverage"] = "beer"
        m1["location"] = "Mells"

        val list = m1.map{ it.value + " rocks" }

        println("Got new list $list")
        assertEquals(arrayList("beer rocks", "Mells rocks"), list)
    }

    test fun mapValues() {
        val m1 = TreeMap<String, String>()
        m1["beverage"] = "beer"
        m1["location"] = "Mells"

        val m2 = m1.mapValues{ it.value + "2" }

        println("Got new map $m2")
        assertEquals(arrayList("beer2", "Mells2"), m2.values().toList())
    }

    test fun createSortedMap() {
        val map = sortedMapOf("c" to 3, "b" to 2, "a" to 1)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
        assertEquals(3, map.get("c"))
        assertEquals(arrayList("a", "b", "c"), map.keySet()!!.toList())
    }

    test fun toSortedMap() {
        val map = hashMapOf<String,Int>("c" to 3, "b" to 2, "a" to 1)
        val sorted = map.toSortedMap<String,Int>()
        assertEquals(1, sorted.get("a"))
        assertEquals(2, sorted.get("b"))
        assertEquals(3, sorted.get("c"))
        assertEquals(arrayList("a", "b", "c"), sorted.keySet()!!.toList())
    }

    test fun toSortedMapWithComparator() {
        val map = hashMapOf("c" to 3, "bc" to 2, "bd" to 4, "abc" to 1)
        val c = comparator<String>{ a, b ->
            val answer = a.length() - b.length()
            if (answer == 0) a.compareTo(b) else answer
        }
        val sorted = map.toSortedMap(c)
        assertEquals(arrayList("c", "bc", "bd", "abc"), sorted.keySet()!!.toList())
        assertEquals(1, sorted.get("abc"))
        assertEquals(2, sorted.get("bc"))
        assertEquals(3, sorted.get("c"))
    }

    test fun compilerBug() {
        val map = TreeMap<String, String>()
        map["beverage"] = "beer"
        map["location"] = "Mells"
        map["name"] = "James"

        var list = arrayList<String>()
        for (e in map) {
            println("key = ${e.getKey()}, value = ${e.getValue()}")
            list += e.getKey()
            list += e.getValue()
        }

        assertEquals(6, list.size())
        assertEquals("beverage,beer,location,Mells,name,James", list.joinToString(","))
        println("==== worked! $list")
    }
    */

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
