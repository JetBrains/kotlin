package test.collections

import kotlin.test.*

import java.util.*
import junit.framework.TestCase
import org.junit.Test

class MapTest {

    Test fun getOrElse() {
        val data = HashMap<String, Int>()
        val a = data.getOrElse("foo"){2}
        assertEquals(2, a)

        val b = data.getOrElse("foo"){3}
        assertEquals(3, b)
        assertEquals(0, data.size())
    }

    Test fun getOrPut() {
        val data = HashMap<String, Int>()
        val a = data.getOrPut("foo"){2}
        assertEquals(2, a)

        val b = data.getOrPut("foo"){3}
        assertEquals(2, b)

        assertEquals(1, data.size())
    }

    Test fun sizeAndEmpty() {
        val data = HashMap<String, Int>()
        assertTrue{ data.empty }
        assertEquals(data.size, 0)
    }

    Test fun setViaIndexOperators() {
        val map = HashMap<String, String>()
        assertTrue{ map.empty }
        assertEquals(map.size, 0)

        map["name"] = "James"

        assertTrue{ !map.empty }
        assertEquals(map.size(), 1)
        assertEquals("James", map["name"])
    }
}
