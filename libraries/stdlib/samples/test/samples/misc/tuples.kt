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

}