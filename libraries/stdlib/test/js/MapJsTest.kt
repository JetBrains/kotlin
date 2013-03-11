package testPackage

import kotlin.test.*

import java.util.*
import org.junit.Test as test

class MapJsTest {
    //TODO: replace `array(...).toList()` to `listOf(...)`
    val KEYS = array("zero", "one", "two", "three").toList()
    val VALUES = array(0, 1, 2, 3).toList()

    test fun getOrElse() {
        val data = HashMap<String, Int>()
        val a = data.getOrElse("foo"){2}
        assertEquals(2, a)

        val b = data.getOrElse("foo"){3}
        assertEquals(3, b)
        assertEquals(0, data.size())
    }

    test fun getOrPut() {
        val data = HashMap<String, Int>()
        val a = data.getOrPut("foo"){2}
        assertEquals(2, a)

        val b = data.getOrPut("foo"){3}
        assertEquals(2, b)

        assertEquals(1, data.size())
    }

    test fun sizeAndEmpty() {
        val data = HashMap<String, Int>()
        assertTrue{ data.empty }
        assertEquals(data.size, 0)
    }

    // #KT-3035
    test fun emptyHashMapValues() {
        val emptyMap = HashMap<String, Int>()
        assertTrue(emptyMap.values().isEmpty())
    }

    test fun hashMapValues() {
        val map = createTestHashMap()
        assertEquals(VALUES, map.values().toSortedList())
    }

    test fun hashMapKeySet() {
        val map = createTestHashMap()
        assertEquals(KEYS.toSortedList(), map.keySet().toSortedList())
    }

    test fun hashMapContainsValue() {
        val map = createTestHashMap()

        assertTrue(map.containsValue(VALUES[0]) &&
            map.containsValue(VALUES[1]) &&
            map.containsValue(VALUES[2]) &&
            map.containsValue(VALUES[3]))

        assertFalse(map.containsValue("four") ||
            map.containsValue("five"))
    }

    test fun hashMapSize() {
        val map = createTestHashMap()
        assertEquals(KEYS.size, map.size)
    }

    test fun hashMapPutAll() {
        val map = createTestHashMap()
        val newMap = HashMap<String, Int>()
        newMap.putAll(map)
        assertEquals(KEYS.size, newMap.size)
    }


    fun createTestHashMap(): HashMap<String, Int> {
        val map = HashMap<String, Int>()
        for (i in KEYS.indices) {
            map.put(KEYS[i], VALUES[i])
        }
        return map
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

    test fun createUsingTuples() {
        val map = hashMap(Pair("a", 1), Pair("b", 2))
        assertEquals(2, map.size)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
    }

    test fun createUsingTo() {
        val map = hashMap("a" to 1, "b" to 2)
        assertEquals(2, map.size)
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
    }

    /*
    test fun createLinkedMap() {
        val map = linkedMap(#("c", 3), #("b", 2), #("a", 1))
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
        assertEquals("beverage,beer,location,Mells,name,James", list.makeString(","))
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
        assertEquals("beverage,beer,location,Mells,name,James", list.makeString(","))
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
        val map = sortedMap(#("c", 3), #("b", 2), #("a", 1))
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
        assertEquals(3, map.get("c"))
        assertEquals(arrayList("a", "b", "c"), map.keySet()!!.toList())
    }

    test fun toSortedMap() {
        val map = hashMap<String,Int>(#("c", 3), #("b", 2), #("a", 1))
        val sorted = map.toSortedMap<String,Int>()
        assertEquals(1, sorted.get("a"))
        assertEquals(2, sorted.get("b"))
        assertEquals(3, sorted.get("c"))
        assertEquals(arrayList("a", "b", "c"), sorted.keySet()!!.toList())
    }

    test fun toSortedMapWithComparator() {
        val map = hashMap(#("c", 3), #("bc", 2), #("bd", 4), #("abc", 1))
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
        assertEquals("beverage,beer,location,Mells,name,James", list.makeString(","))
        println("==== worked! $list")
    }
    */

}
