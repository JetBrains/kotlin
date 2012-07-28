package test.collections

import kotlin.test.*

import java.util.*
import org.junit.Test as test

class MapTest {

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

    test fun setViaIndexOperators() {
        val map = HashMap<String, String>()
        assertTrue{ map.empty }
        assertEquals(map.size, 0)

        map["name"] = "James"

        assertTrue{ !map.empty }
        assertEquals(map.size(), 1)
        assertEquals("James", map["name"])
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

    /*
        TODO compiler bug
        we should be able to remove the explicit type <String,String,String> on the map function
        http://youtrack.jetbrains.net/issue/KT-1145
    */
    test fun map() {
        val m1 = TreeMap<String, String>()
        m1["beverage"] = "beer"
        m1["location"] = "Mells"

        val list = m1.map<String,String,String>{ it.value + " rocks" }

        println("Got new list $list")
        assertEquals(arrayList("beer rocks", "Mells rocks"), list)
    }

    /*
        TODO compiler bug
        we should be able to remove the explicit type <String,String,String> on the mapValues function
        http://youtrack.jetbrains.net/issue/KT-1145
    */
    test fun mapValues() {
        val m1 = TreeMap<String, String>()
        m1["beverage"] = "beer"
        m1["location"] = "Mells"

        val m2 = m1.mapValues<String,String,String>{ it.value + "2" }

        println("Got new map $m2")
        assertEquals(arrayList("beer2", "Mells2"), m2.values().toList())
    }

    test fun createUsingTuples() {
        val map = hashMap(#("a", 1), #("b", 2))
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

    test fun createLinkedMap() {
        val map = linkedMap(#("c", 3), #("b", 2), #("a", 1))
        assertEquals(1, map.get("a"))
        assertEquals(2, map.get("b"))
        assertEquals(3, map.get("c"))
        assertEquals(arrayList("c", "b", "a"), map.keySet().toList())
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

    /**
    TODO
    test case for http://youtrack.jetbrains.com/issue/KT-1773

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
