package test.collections

import kotlin.test.*

import java.util.*
import org.junit.Test  as test

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

        var list = arrayList<String>()
        for (e in map) {
            println("key = ${e.getKey()}, value = ${e.getValue()}")

            list.add(e.getKey())
            list.add(e.getValue())
/*
            // TODO compiler bug
            list += e.getKey()
            list += e.getValue()
*/
        }

        assertEquals(6, list.size())
        assertEquals("beverage,beer,location,Mells,name,James", list.makeString(","))
    }

    /*
    TODO compiler bug

    test fun iteratorWithProperties() {
        val map = TreeMap<String, String>()
        map["beverage"] = "beer"
        map["location"] = "Mells"
        map["name"] = "James"

        var list = arrayList<String>()
        for (e in map) {
            println("got $e")
            println("key = ${e.key}, value = ${e.value}")
            list += e.key
            list += e.value
        }

        assertEquals(6, list.size())
        assertEquals("beverage,beer,location,Mells,name,James", list.makeString(","))
    }
    */
}
