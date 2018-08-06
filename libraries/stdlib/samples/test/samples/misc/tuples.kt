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
        val list = Pair(1, "a").toList()
        assertPrints(list, "[1, a]")
        assertTrue(list[0] is Int)
        assertTrue(list[1] is String)
        assertFalse(list[1] is Int)
    }

    @Sample
    fun tripleToList() {
        val list = Triple(1, "a", 0.5).toList()
        assertPrints(list, "[1, a, 0.5]")
        assertTrue(list[0] is Int)
        assertTrue(list[1] is String)
        assertTrue(list[2] is Double)
        assertFalse(list[2] is String)
    }

}