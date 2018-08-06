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

}