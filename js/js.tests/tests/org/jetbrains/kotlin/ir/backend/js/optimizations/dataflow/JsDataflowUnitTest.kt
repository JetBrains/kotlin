/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Lattice algebra and solver termination unit tests (no IR pipeline).
 */
class JsDataflowUnitTest {

    @Test
    fun constKindDistinguishesCharFromInt() {
        val charA = JsValueLattice.Const(IrConstKind.Char, 'A')
        val int65 = JsValueLattice.Const(IrConstKind.Int, 65)
        assertNotEquals(charA, int65)
        assertEquals(JsValueLattice.Top, charA.join(int65))
    }

    @Test
    fun valueJoinIsIdempotentAndCommutative() {
        val a = JsValueLattice.Const(IrConstKind.Int, 1)
        val b = JsValueLattice.Const(IrConstKind.Int, 2)
        assertEquals(a, a.join(a))
        assertEquals(a.join(b), b.join(a))
        assertEquals(a, JsValueLattice.Bottom.join(a))
        assertEquals(JsValueLattice.Top, a.join(JsValueLattice.Top))
    }

    @Test
    fun factEnvLatticeJoin() {
        val env1 = FactEnv()
        val env2 = FactEnv()
        assertTrue(FactEnv.lattice.equivalent(env1, env2))
        assertTrue(FactEnv.lattice.equivalent(FactEnv.lattice.join(env1, env2), env1))
    }

    @Test
    fun solverConvergesOnFiniteLattice() {
        val a = JsBasicBlock(0)
        val b = JsBasicBlock(1)
        a.terminator = JsTerminator.Goto(b)
        a.successors += b
        b.predecessors += a
        b.terminator = JsTerminator.Return(null)

        val lattice = object : Lattice<Int> {
            override fun bottom(): Int = 0
            override fun join(a: Int, b: Int): Int = maxOf(a, b)
            override fun equivalent(a: Int, b: Int): Boolean = a == b
        }
        val result = ForwardDataflowSolver().solve(
            entry = a,
            entryState = 1,
            lattice = lattice,
            transfer = { block, inn -> block.successors.associateWith { inn + 1 } },
            iterationBudget = 32,
        )
        assertEquals(1, result[a])
        assertEquals(2, result[b])
    }

    @Test
    fun solverBudgetIsAssertionNotSilentCap() {
        val a = JsBasicBlock(0)
        val b = JsBasicBlock(1)
        a.terminator = JsTerminator.Goto(b)
        a.successors += b
        b.predecessors += a
        b.terminator = JsTerminator.Goto(a)
        b.successors += a
        a.predecessors += b

        val lattice = object : Lattice<Int> {
            override fun bottom(): Int = 0
            override fun join(a: Int, b: Int): Int = a + b + 1
            override fun equivalent(a: Int, b: Int): Boolean = false
        }

        assertThrows<IllegalStateException> {
            ForwardDataflowSolver().solve(
                entry = a,
                entryState = 0,
                lattice = lattice,
                transfer = { block, inn -> block.successors.associateWith { inn + 1 } },
                iterationBudget = 8,
                debugName = "cycle",
            )
        }
    }
}
