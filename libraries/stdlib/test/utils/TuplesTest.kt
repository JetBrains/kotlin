/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

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

class QuadrupleTest {
    val t = Quadruple(1, "a", 0.07, 2L)

    @Test fun quadrupleFirstAndSecond() {
        assertEquals(1, t.first)
        assertEquals("a", t.second)
        assertEquals(0.07, t.third)
        assertEquals(2L, t.fourth)
    }

    @Test fun quadrupleMultiAssignment() {
        val (a, b, c, d) = t
        assertEquals(1, a)
        assertEquals("a", b)
        assertEquals(0.07, c)
        assertEquals(2L, d)
    }

    @Test fun quadrupleToString() {
        assertEquals("(1, a, 0.07, 2)", t.toString())
    }

    @Test fun quadrupleEquals() {
        assertEquals(Quadruple(1, "a", 0.07, 2L), t)
        assertNotEquals(Quadruple(2, "a", 0.07, 2L), t)
        assertNotEquals(Quadruple(1, "b", 0.07, 2L), t)
        assertNotEquals(Quadruple(1, "a", 0.1, 2L), t)
        assertNotEquals(Quadruple(1, "a", 0.07, 3L), t)
        assertTrue(!t.equals(null))
        assertNotEquals("", (t as Any))
    }

    @Test fun quadrupleHashCode() {
        assertEquals(Quadruple(1, "a", 0.07, 2L).hashCode(), t.hashCode())
        assertNotEquals(Quadruple(2, "a", 0.07, 2L).hashCode(), t.hashCode())
        assertNotEquals(0, Quadruple(null, "b", 0.07, 2L).hashCode())
        assertNotEquals(0, Quadruple("b", null, 0.07, 2L).hashCode())
        assertNotEquals(0, Quadruple("b", 1, null, 2L).hashCode())
        assertNotEquals(0, Quadruple("b", 1, 0.07, null).hashCode())
        assertEquals(0, Quadruple(null, null, null, null).hashCode())
    }

    @Test fun quadrupleHashSet() {
        val s = hashSetOf(Quadruple(1, "a", 0.07, 2L), Quadruple(1, "b", 0.07, 2L), Quadruple(1, "a", 0.07, 2L))
        assertEquals(2, s.size)
        assertTrue(s.contains(t))
    }

    @Test fun quadrupleToList() {
        assertEquals(listOf(1, 2, 3, 4), (Quadruple(1, 2, 3, 4)).toList())
        assertEquals(listOf(1, null, 3, 4), (Quadruple(1, null, 3, 4)).toList())
        assertEquals(listOf(1, 2, "3", 4), (Quadruple(1, 2, "3", 4)).toList())
    }
}
