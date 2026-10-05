/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.opt

import hair.graph.InequalityGraph
import hair.inequality.Bound
import hair.inequality.MinVertex
import hair.inequality.PhiVertex
import hair.inequality.Vertex
import hair.inequality.VertexKey
import hair.opt.DemandProver
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemandProverTest {
    private class G {
        val graph = InequalityGraph()
        val zero = MinVertex(VertexKey.Zero, synthesized = false).also(graph::add)
        val len = MinVertex(VertexKey.Length(1000), synthesized = false).also(graph::add)
        private var next = 1

        fun min() = MinVertex(VertexKey.Value(next++), synthesized = false).also(graph::add)
        fun phi() = PhiVertex(VertexKey.Value(next++)).also(graph::add)

        fun upper(from: Vertex, to: Vertex, w: Long) = to.addPred(Bound.UPPER, from, w)
        fun lower(from: Vertex, to: Vertex, w: Long) = to.addPred(Bound.LOWER, from, w)
        fun flow(from: Vertex, to: Vertex, w: Long) {
            upper(from, to, w)
            lower(from, to, w)
        }

        fun provesUpper(target: Vertex, c: Long, source: Vertex = len) =
            DemandProver(Bound.UPPER).prove(source, target, c)

        fun provesLower(target: Vertex, c: Long = 0, source: Vertex = zero) =
            DemandProver(Bound.LOWER).prove(source, target, c)
    }

    @Test
    fun singleEdgeProvesExactlyItsBounds() = with(G()) {
        val i = min()
        upper(len, i, -1)       // i <= len - 1
        assertTrue(provesUpper(i, -1))
        assertTrue(provesUpper(i, 0), "a weaker bound follows from a stronger one")
        assertFalse(provesUpper(i, -2), "a stronger bound does not")
    }

    @Test
    fun noPathProvesNothing() = with(G()) {
        val i = min()
        assertFalse(provesUpper(i, -1))
        assertFalse(provesLower(i))
    }

    @Test
    fun boundsUseTheirOwnEdges() = with(G()) {
        val i = min()
        // i <= 0
        upper(zero, i, 0)
        assertFalse(provesLower(i), "an upper edge must not prove i >= 0")
    }

    @Test
    fun phiNeedsEveryOperand() = with(G()) {
        val x = min()
        val y = min()
        val p = phi()

        flow(x, p, 0)
        flow(y, p, 0)
        // only x >= 0 is known
        lower(zero, x, 0)
        assertFalse(provesLower(p))
        lower(zero, y, 0)
        assertTrue(provesLower(p))
    }

    /** `for (j = 0; j < len; j++)` */
    @Test
    fun countingUpLoop() = with(G()) {
        val j1 = phi()
        val j2 = min()
        val j3 = min()
        val lim = min()

        // j1 = φ(0, j3)
        flow(zero, j1, 0)
        flow(j3, j1, 0)
        // j2 = π(j1)
        flow(j1, j2, 0)
        // lim = π(len)
        flow(len, lim, 0)
        // j2 < lim
        upper(lim, j2, -1)
        lower(j2, lim, 1)
        // j3 = j2 + 1
        flow(j2, j3, 1)

        assertTrue(provesUpper(j2, -1), "upper bound from the loop condition")
        assertTrue(provesLower(j2), "lower bound through a harmless (increasing) cycle")
    }

    /** for `j = len - 1; ; j==) */
    @Test
    fun countingDownLoopHasNoLowerBound() = with(G()) {
        val init = min()
        val j1 = phi()
        val j2 = min()
        val j3 = min()

        // 0 <= init <= len - 1
        upper(len, init, -1)
        lower(zero, init, 0)
        flow(init, j1, 0)
        flow(j3, j1, 0)
        flow(j1, j2, 0)
        // j3 = j2 - 1
        flow(j2, j3, -1)

        assertTrue(provesUpper(j2, -1), "decreasing cycle is harmless for the upper bound")
        assertFalse(provesLower(j2), "decreasing cycle amplifies for the lower bound")
    }

    @Test
    fun harmlessCycleThroughPhi() = with(G()) {
        val l0 = min()
        val l1 = phi()
        val l2 = min()
        val l3 = min()
        val l4 = min()
        val j = min()

        upper(len, l0, 0)
        // l1 = φ(l0, l3)
        upper(l0, l1, 0)
        upper(l3, l1, 0)
        // l2 = π(l1)
        upper(l1, l2, 0)
        // l3 = l2 - 1
        upper(l2, l3, -1)
        // l4 = π(l3)
        upper(l3, l4, 0)
        // j < l4
        upper(l4, j, -1)

        assertTrue(provesUpper(j, -1))
    }

    @Test
    fun phiFreeCycleCarriesNoBound() = with(G()) {
        val a = min()
        val b = min()
        val t = min()
        val src = zero

        // a == b, a cycle with no no φ
        upper(a, b, 0)
        upper(b, a, 0)
        upper(b, t, 0)
        // a <= 0 + 5
        upper(src, a, 5)

        assertFalse(provesUpper(t, 4, src), "the cycle alone must not prove anything")
        assertTrue(provesUpper(t, 5, src))
    }
}
