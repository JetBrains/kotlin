package samples.misc

import samples.*
import kotlin.test.*

class Tuples {

    @Sample
    fun pairDestructuring() {
        val (a, b) = Pair(1, "x")
        assertPrints(a, "1")
        assertPrints(b, "x")
    }

    @Sample
    fun tripleDestructuring() {
        val (a, b, c) = Triple(2, "x", listOf(null))
        assertPrints(a, "2")
        assertPrints(b, "x")
        assertPrints(c, "[null]")
    }

    @Sample
    fun pairToList() {
        val mixedList: List<Any> = Pair(1, "a").toList()
        assertPrints(mixedList, "[1, a]")
        assertTrue(mixedList[0] is Int)
        assertTrue(mixedList[1] is String)

        val intList: List<Int> = Pair(0, 1).toList()
        assertPrints(intList, "[0, 1]")
    }

    @Sample
    fun tripleToList() {
        val mixedList: List<Any> = Triple(1, "a", 0.5).toList()
        assertPrints(mixedList, "[1, a, 0.5]")
        assertTrue(mixedList[0] is Int)
        assertTrue(mixedList[1] is String)
        assertTrue(mixedList[2] is Double)

        val intList: List<Int> = Triple(0, 1, 2).toList()
        assertPrints(intList, "[0, 1, 2]")
    }

    @Sample
    fun pairMapFirst() {
        val x = Pair(1, "hello").mapFirst { it + 100 }
        assertPrints(x, "(101, hello)")
        assertEquals(Pair(101, "hello"), x)
    }

    @Sample
    fun pairMapSecond() {
        val x = Pair(1, "hello").mapSecond { it.length }
        assertPrints(x, "(1, 5)")
        assertEquals(Pair(1, 5), x)
    }

    @Sample
    fun tripleMapFirst() {
        val x = Triple(1, "hello", 3.14).mapFirst { it + 100 }
        assertPrints(x, "(101, hello, 3.14)")
        assertEquals(Triple(101, "hello", 3.14), x)
    }


    @Sample
    fun tripleMapSecond() {
        val x = Triple(1, "hello", 3.14).mapSecond { it.length }
        assertPrints(x, "(1, 5, 3.14)")
        assertEquals(Triple(1, 5, 3.14), x)
    }

    @Sample
    fun tripleMapThird() {
        val x = Triple(1, "hello", 3.14).mapThird { it.toString().reversed() }
        assertPrints(x, "(1, hello, 41.3)")
        assertEquals(Triple(1, "hello", "41.3"), x)
    }

}