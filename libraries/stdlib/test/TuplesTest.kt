package test.tuples

import kotlin.test.assertTrue
import org.junit.Test as test
import kotlin.test.assertEquals

class PairTest {
    val p = Pair(1, "a")

    test fun pairFirstAndSecond() {
        assertEquals(1, p.first)
        assertEquals("a", p.second)
    }

    test fun pairMultiAssignment() {
        val (a, b) = p
        assertEquals(1, a)
        assertEquals("a", b)
    }

    test fun pairToString() {
        assertEquals("(1, a)", p.toString())
    }

    test fun pairEquals() {
        assertTrue(p == Pair(1, "a"))
        assertTrue(p != Pair(2, "a"))
        assertTrue(p != Pair(1, "b"))
        assertTrue(!(p : Object).equals(null))
        assertTrue((p : Any) != "")
    }

    test fun pairHashCode() {
        assertTrue(p.hashCode() == Pair(1, "a").hashCode())
        assertTrue(p.hashCode() != Pair(2, "a").hashCode())
        assertTrue(Pair(null, "b").hashCode() != 0)
        assertTrue(Pair("b", null).hashCode() != 0)
        assertTrue(Pair(null, null).hashCode() == 0)
    }

    test fun pairHashSet() {
        val s = hashSet(Pair(1, "a"), Pair(1, "b"), Pair(1, "a"))
        assertTrue(s.size == 2)
        assertTrue(s.contains(p))
    }
}

class TripleTest {
    val t = Triple(1, "a", 0.0)

    test fun tripleFirstAndSecond() {
        assertTrue(t.first == 1)
        assertTrue(t.second == "a")
        assertTrue(t.third == 0.0)
    }

    test fun tripleMultiAssignment() {
        val (a, b, c) = t
        assertTrue(a == 1)
        assertTrue(b == "a")
        assertTrue(c == 0.0)
    }

    test fun tripleToString() {
        assertEquals("(1, a, 0.0)", t.toString())
    }

    test fun tripleEquals() {
        assertTrue(t == Triple(1, "a", 0.0))
        assertTrue(t != Triple(2, "a", 0.0))
        assertTrue(t != Triple(1, "b", 0.0))
        assertTrue(t != Triple(1, "a", 0.1))
        assertTrue(!(t : Object).equals(null))
        assertTrue((t : Any) != "")
    }

    test fun tripleHashCode() {
        assertTrue(t.hashCode() == Triple(1, "a", 0.0).hashCode())
        assertTrue(t.hashCode() != Triple(2, "a", 0.0).hashCode())
        assertTrue(Triple(null, "b", 0.0).hashCode() != 0)
        assertTrue(Triple("b", null, 0.0).hashCode() != 0)
        assertTrue(Triple("b", 1, null).hashCode() != 0)
        assertTrue(Triple(null, null, null).hashCode() == 0)
    }

    test fun tripleHashSet() {
        val s = hashSet(Triple(1, "a", 0.0), Triple(1, "b", 0.0), Triple(1, "a", 0.0))
        assertTrue(s.size == 2)
        assertTrue(s.contains(t))
    }
}
