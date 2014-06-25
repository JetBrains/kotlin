package testPackage

import kotlin.test.*

import java.util.*
import org.junit.Test as test

class ComplexMapJsTest : MapJsTest() {
    // Helper function with generic parameter to force to use ComlpexHashMap
    fun doTest<K : kotlin.Comparable<K>>() {
        HashMap<K, Int>()
        HashMap<K, Int>(3)
        HashMap<K, Int>(3, 0.5f)
        val map = HashMap<K, Int>(createTestMap() as HashMap<K, Int>)

        assertEquals(KEYS.toNormalizedList(), map.keySet().toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), map.values().toNormalizedList())
    }
    test override fun constructors() {
        doTest<String>()
    }

    override fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T> = this.toSortedList()
    // hashMapOf returns ComlpexHashMap because it is Generic
    override fun emptyMutableMap(): MutableMap<String, Int> = hashMapOf()
}

class PrimitiveMapJsTest : MapJsTest() {
    test override fun constructors() {
        HashMap<String, Int>()
        HashMap<String, Int>(3)
        HashMap<String, Int>(3, 0.5f)

        val map = HashMap<String, Int>(createTestMap())

        assertEquals(KEYS.toNormalizedList(), map.keySet().toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), map.values().toNormalizedList())
    }

    override fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T> = this.toSortedList()
    override fun emptyMutableMap(): MutableMap<String, Int> = HashMap()
}

class LinkedHashMapTest : MapJsTest() {
    test override fun constructors() {
        LinkedHashMap<String, Int>()
        LinkedHashMap<String, Int>(3)
        LinkedHashMap<String, Int>(3, 0.5f)

        val map = LinkedHashMap<String, Int>(createTestMap())

        assertEquals(KEYS.toNormalizedList(), map.keySet().toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), map.values().toNormalizedList())
    }

    override fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T> = this.toList()
    override fun emptyMutableMap(): MutableMap<String, Int> = LinkedHashMap()
}

abstract class MapJsTest {
    //TODO: replace `array(...).toList()` to `listOf(...)`
    val KEYS = array("zero", "one", "two", "three").toList()
    val VALUES = array(0, 1, 2, 3).toList()

    val SPECIAL_NAMES = array("__proto__", "constructor", "toString", "toLocaleString", "valueOf", "hasOwnProperty", "isPrototypeOf", "propertyIsEnumerable")

    test fun getOrElse() {
        val data = emptyMap()
        val a = data.getOrElse("foo"){2}
        assertEquals(2, a)

        val b = data.getOrElse("foo"){3}
        assertEquals(3, b)
        assertEquals(0, data.size())
    }

    test fun getOrPut() {
        val data = emptyMutableMap()
        val a = data.getOrPut("foo"){2}
        assertEquals(2, a)

        val b = data.getOrPut("foo"){3}
        assertEquals(2, b)

        assertEquals(1, data.size())
    }

    test fun emptyMapGet() {
        val map = emptyMap()
        assertEquals(null, map.get("foo"), """failed on map.get("foo")""")
        assertEquals(null, map["bar"], """failed on map["bar"]""")
    }

    test fun mapGet() {
        val map = createTestMap()
        for (i in KEYS.indices) {
            assertEquals(VALUES[i], map.get(KEYS[i]), """failed on map.get(KEYS[$i])""")
            assertEquals(VALUES[i], map[KEYS[i]], """failed on map[KEYS[$i]]""")
        }

        assertEquals(null, map.get("foo"))
    }

    /* TODO: fix after switch to use compiled stdlib (need drop js.Map<K,V>.set(V))
    test fun mapPut() {
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
    */

    test fun sizeAndEmptyForEmptyMap() {
        val data = emptyMap()

        assertTrue(data.isEmpty())
        assertTrue(data.empty)

        assertEquals(0, data.size())
        assertEquals(0, data.size)
    }

    test fun sizeAndEmpty() {
        val data = createTestMap()

        assertFalse(data.isEmpty())
        assertFalse(data.empty)

        assertEquals(KEYS.size, data.size())
        assertEquals(KEYS.size, data.size)
    }

    // #KT-3035
    test fun emptyMapValues() {
        val emptyMap = emptyMap()
        assertTrue(emptyMap.values().isEmpty())
    }

    test fun mapValues() {
        val map = createTestMap()
        assertEquals(VALUES.toNormalizedList(), map.values().toNormalizedList())
    }

    test fun mapKeySet() {
        val map = createTestMap()
        assertEquals(KEYS.toNormalizedList(), map.keySet().toNormalizedList())
    }

    test fun mapEntrySet() {
        val map = createTestMap()

        val actualKeys = ArrayList<String>()
        val actualValues = ArrayList<Int>()
        for (e in map.entrySet()) {
            actualKeys.add(e.getKey())
            actualValues.add(e.getValue())
        }

        assertEquals(KEYS.toNormalizedList(), actualKeys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), actualValues.toNormalizedList())
    }

    test fun mapContainsKey() {
        val map = createTestMap()

        assertTrue(map.containsKey(KEYS[0]) &&
                   map.containsKey(KEYS[1]) &&
                   map.containsKey(KEYS[2]) &&
                   map.containsKey(KEYS[3]))

        assertFalse(map.containsKey("foo") ||
                    map.containsKey(1))
    }

    test fun mapContainsValue() {
        val map = createTestMap()

        assertTrue(map.containsValue(VALUES[0]) &&
                   map.containsValue(VALUES[1]) &&
                   map.containsValue(VALUES[2]) &&
                   map.containsValue(VALUES[3]))

        assertFalse(map.containsValue("four") ||
                    map.containsValue(5))
    }

    test fun mapPutAll() {
        val map = createTestMap()
        val newMap = emptyMutableMap()
        newMap.putAll(map)
        assertEquals(KEYS.size, newMap.size)
    }

    test fun mapRemove() {
        val map = createTestMutableMap()
        val last = KEYS.size() - 1
        val first = 0
        val mid = KEYS.size() / 2

        assertEquals(KEYS.size(), map.size())

        assertEquals(null, map.remove("foo"))
        assertEquals(VALUES[mid], map.remove(KEYS[mid]))
        assertEquals(null, map.remove(KEYS[mid]))
        assertEquals(VALUES[last], map.remove(KEYS[last]))
        assertEquals(VALUES[first], map.remove(KEYS[first]))

        assertEquals(KEYS.size() - 3, map.size())
    }

    test fun mapClear() {
        val map = createTestMutableMap()
        assertFalse(map.isEmpty())
        map.clear()
        assertTrue(map.isEmpty())
    }

    /*

    TODO fix bug with .set() on Map...

    test fun setViaIndexOperators() {
        val map = HashMap<String, String>()
        assertTrue{ map.empty }
        assertEquals(map.size, 0)

        map["name"] = "James"

        assertTrue{ !map.empty }
        assertEquals(map.size(), 1)
        assertEquals("James", map["name"])
    }
    */

    test fun createUsingPairs() {
        val map = mapOf(Pair("a", 1), Pair("b", 2))
        assertEquals(2, map.size)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
    }

    test fun createUsingTo() {
        val map = mapOf("a" to 1, "b" to 2)
        assertEquals(2, map.size)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
    }

    test fun mapIteratorImplicitly() {
        val map = createTestMap()

        val actualKeys = ArrayList<String>()
        val actualValues = ArrayList<Int>()
        for (e in map) {
            actualKeys.add(e.getKey())
            actualValues.add(e.getValue())
        }

        assertEquals(KEYS.toNormalizedList(), actualKeys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), actualValues.toNormalizedList())
    }

    test fun mapIteratorExplicitly() {
        val map = createTestMap()

        val actualKeys = ArrayList<String>()
        val actualValues = ArrayList<Int>()
        val iterator = map.iterator()
        for (e in iterator) {
            actualKeys.add(e.getKey())
            actualValues.add(e.getValue())
        }

        assertEquals(KEYS.toNormalizedList(), actualKeys.toNormalizedList())
        assertEquals(VALUES.toNormalizedList(), actualValues.toNormalizedList())
    }

    test fun specialNamesNotContainsInEmptyMap() {
        val map = emptyMap()

        for (key in SPECIAL_NAMES) {
            assertFalse(map.containsKey(key), "unexpected key: $key")
        }
    }

    test fun specialNamesNotContainsInNonEmptyMap() {
        val map = createTestMap()

        for (key in SPECIAL_NAMES) {
            assertFalse(map.containsKey(key), "unexpected key: $key")
        }
    }

    test fun putAndGetSpecialNamesToMap() {
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

    test abstract fun constructors()

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

    // Helpers

    abstract fun <T : kotlin.Comparable<T>> Collection<T>.toNormalizedList(): List<T>

    fun emptyMap(): Map<String, Int> = emptyMutableMap()

    abstract fun emptyMutableMap(): MutableMap<String, Int>

    fun createTestMap(): Map<String, Int> = createTestMutableMap()

    fun createTestMutableMap(): MutableMap<String, Int> {
        val map = emptyMutableMap()
        for (i in KEYS.indices) {
            map.put(KEYS[i], VALUES[i])
        }
        return map
    }
}
