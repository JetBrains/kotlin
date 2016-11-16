package test.tuples

import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PairTest {
    val p = Pair(1, "a")

    @Test fun pairFirstAndSecond() {
        assertEquals(1, p.first)
        assertEquals("a", p.second)
    }

    @Test fun pairMultiAssignment() {
        val (a, b) = p
        assertEquals(1, a)
        assertEquals("a", b)
    }

    @Test fun pairToString() {
        assertEquals("(1, a)", p.toString())
    }

    @Test fun pairEquals() {
        assertEquals(Pair(1, "a"), p)
        assertNotEquals(Pair(2, "a"), p)
        assertNotEquals(Pair(1, "b"), p)
        assertTrue(!p.equals(null))
        assertNotEquals("", (p as Any))
    }

    @Test fun pairHashCode() {
        assertEquals(Pair(1, "a").hashCode(), p.hashCode())
        assertNotEquals(Pair(2, "a").hashCode(), p.hashCode())
        assertNotEquals(0, Pair(null, "b").hashCode())
        assertNotEquals(0, Pair("b", null).hashCode())
        assertEquals(0, Pair(null, null).hashCode())
    }

    @Test fun pairHashSet() {
        val s = hashSetOf(Pair(1, "a"), Pair(1, "b"), Pair(1, "a"))
        assertEquals(2, s.size)
        assertTrue(s.contains(p))
    }

    @Test fun pairToList() {
        assertEquals(listOf(1, 2), (1 to 2).toList())
        assertEquals(listOf(1, null), (1 to null).toList())
        assertEquals(listOf(1, "2"), (1 to "2").toList())
    }
}

class TripleTest {
    val t = Triple(1, "a", 0.07)

    @Test fun tripleFirstAndSecond() {
        assertEquals(1, t.first)
        assertEquals("a", t.second)
        assertEquals(0.07, t.third)
    }

    @Test fun tripleMultiAssignment() {
        val (a, b, c) = t
        assertEquals(1, a)
        assertEquals("a", b)
        assertEquals(0.07, c)
    }

    @Test fun tripleToString() {
        assertEquals("(1, a, 0.07)", t.toString())
    }

    @Test fun tripleEquals() {
        assertEquals(Triple(1, "a", 0.07), t)
        assertNotEquals(Triple(2, "a", 0.07), t)
        assertNotEquals(Triple(1, "b", 0.07), t)
        assertNotEquals(Triple(1, "a", 0.1), t)
        assertTrue(!t.equals(null))
        assertNotEquals("", (t as Any))
    }

    @Test fun tripleHashCode() {
        assertEquals(Triple(1, "a", 0.07).hashCode(), t.hashCode())
        assertNotEquals(Triple(2, "a", 0.07).hashCode(), t.hashCode())
        assertNotEquals(0, Triple(null, "b", 0.07).hashCode())
        assertNotEquals(0, Triple("b", null, 0.07).hashCode())
        assertNotEquals(0, Triple("b", 1, null).hashCode())
        assertEquals(0, Triple(null, null, null).hashCode())
    }

    @Test fun tripleHashSet() {
        val s = hashSetOf(Triple(1, "a", 0.07), Triple(1, "b", 0.07), Triple(1, "a", 0.07))
        assertEquals(2, s.size)
        assertTrue(s.contains(t))
    }

    @Test fun tripleToList() {
        assertEquals(listOf(1, 2, 3), (Triple(1, 2, 3)).toList())
        assertEquals(listOf(1, null, 3), (Triple(1, null, 3)).toList())
        assertEquals(listOf(1, 2, "3"), (Triple(1, 2, "3")).toList())
    }
}
