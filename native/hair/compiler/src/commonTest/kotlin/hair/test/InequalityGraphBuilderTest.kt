/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.test

import hair.graph.InequalityGraph
import hair.graph.InequalityGraphBuilder
import hair.inequality.*
import hair.ir.*
import hair.ir.nodes.*
import hair.sym.CmpOp
import hair.sym.HairType.*
import hair.transform.withValueTypes
import kotlin.random.Random
import kotlin.test.*

class InequalityGraphBuilderTest : BceTest {
    @Test
    fun checkBoundsItsPi() {
        lateinit var check: ArrayIndexCheck
        val fc = bceIR(INT, REFERENCE) {
            check = ArrayIndexCheck(Param(1), Param(0)) as ArrayIndexCheck
            ReturnVoid()
        }
        val g = fc.graph()
        val pi = g.vertexOf(fc.piOf(check, check.index))!!

        assertTrue(pi.hasPred(Bound.UPPER, g.lengthOf(check.array)!!, -1), "π(i) ≤ len - 1")
        assertTrue(pi.hasPred(Bound.LOWER, g.zero!!, 0), "π(i) ≥ 0")
    }

    @Test
    fun lengthAndZeroAreSynthesizedWhenNoNodeProducesThem() {
        lateinit var check: ArrayIndexCheck
        val fc = bceIR(INT, REFERENCE) {
            check = ArrayIndexCheck(Param(1), Param(0)) as ArrayIndexCheck
            ReturnVoid()
        }
        val g = fc.graph()

        assertTrue((g.lengthOf(check.array) as MinVertex).isSynthesized)
        assertTrue((g.zero as MinVertex).isSynthesized)
    }

    @Test
    fun arraySizeAndConstZeroMaterializeThem() {
        lateinit var check: ArrayIndexCheck
        val fc = bceIR(INT, REFERENCE) {
            val a = Param(1)
            check = ArrayIndexCheck(a, Param(0)) as ArrayIndexCheck
            Use(ArraySize(a))
            Use(Const(0))
            ReturnVoid()
        }
        val g = fc.graph()

        assertFalse((g.lengthOf(check.array) as MinVertex).isSynthesized)
        assertFalse((g.zero as MinVertex).isSynthesized)
    }

    @Test
    fun constantsAreTiedToZero() {
        val fc = bceIR {
            Use(Const(10))
            ReturnVoid()
        }
        val g = fc.graph()
        val c = g.vertexOf(fc.session.allNodes<Const>().single())!!
        val zero = g.zero!!

        for (bound in Bound.entries) {
            assertTrue(c.hasPred(bound, zero, 10))
            assertTrue(zero.hasPred(bound, c, -10))
        }
    }

    @Test
    fun extremeConstantsKeepExactWeights() {
        val fc = bceIR {
            Use(Const(Int.MIN_VALUE))
            ReturnVoid()
        }
        val g = fc.graph()
        val c = g.vertexOf(fc.session.allNodes<Const>().single())!!
        val zero = g.zero!!

        for (bound in Bound.entries) {
            assertTrue(c.hasPred(bound, zero, Int.MIN_VALUE.toLong()))
            assertTrue(zero.hasPred(bound, c, 2147483648L))
        }
    }

    /** `if (i < a.size) a[i]`: the branch fact lands on π(i) and π(len), never on i or len */
    @Test
    fun branchFactsDoNotLeak() {
        lateinit var exit: Node
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val a = Param(1)

            branch(Cmp(INT, CmpOp.S_LT)(i, ArraySize(a)), {
                exit = (contextOf<ControlFlowBuilder>().lastControl as BlockEntry).preds.single()
                ArrayIndexCheck(a, i)
            }, {})
            ReturnVoid()
        }
        val g = fc.graph()
        val i = g.vertexOf(fc.session.allNodes<Param>().single { it.index == 0 })!!
        val len = g.vertices.single { it.key is VertexKey.Length }
        val zero = g.zero!!

        for (bound in Bound.entries) assertTrue(i.preds(bound).isEmpty(), "nothing may bound the raw parameter ($bound)")
        assertTrue(len.preds(Bound.UPPER).isEmpty())
        assertEquals(listOf(zero), len.preds(Bound.LOWER).map { it.from }, "only len ≥ 0")

        val piI = g.vertexOf(fc.piOf(exit, fc.session.allNodes<Param>().single { it.index == 0 }))!!
        val piLen = g.vertexOf(fc.session.allNodes<Pi>().single { it.origin == exit && it.value is ArraySize})!!
        assertTrue(piI.hasPred(Bound.UPPER, piLen, -1), "π(i) ≤ π(len) - 1")
        assertTrue(piLen.hasPred(Bound.LOWER, piI, 1), "π(len) ≥ π(i) + 1")
    }

    @Test
    fun unsignedCompareAgainstLengthGivesBothBounds() {
        lateinit var exit: Node
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val a = Param(1)

            branch(Cmp(INT, CmpOp.U_LT)(i, ArraySize(a)), {
                exit = (contextOf<ControlFlowBuilder>().lastControl as BlockEntry).preds.single()
                Use(i)
            }, {})
            ReturnVoid()
        }
        val g = fc.graph()
        val piI = g.vertexOf(fc.piOf(exit, fc.session.allNodes<Param>().single { it.index == 0 }))!!
        val piLen = g.vertexOf(fc.session.allNodes<Pi>().single { it.origin == exit && it.value is ArraySize })!!

        assertTrue(piI.hasPred(Bound.UPPER, piLen, -1), "π(i) ≤ π(len) - 1")
        assertTrue(piI.hasPred(Bound.LOWER, g.zero!!, 0), "π(i) ≥ 0")
    }

    @Test
    fun booleanPhiGetsNoVertex() {
        val fc = bceIR(INT) {
            val b = Var.nextNumbered()

            branch(Cmp(INT, CmpOp.S_LT)(Param(0), Const(0)),
                   {
                       AssignVar(b)(True())
                   }, {
                       AssignVar(b)(False())
                   })
            Use(ReadVar(b))
            ReturnVoid()
        }
        val g = fc.graph()
        val phi = fc.session.allNodes<Phi>().single()
        assertNull(g.vertexOf(phi))
    }

    @Test
    fun visitOrderDoesNotMatter() {
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val a = Param(1)

            ArrayIndexCheck(a, i)
            branch(Cmp(INT, CmpOp.S_LT)(i, ArraySize(a)), { ArrayIndexCheck(a, i) }, {})
            Use(Const(7))
            ReturnVoid()
        }

        fun signature(g: InequalityGraph) = g.vertices.flatMap { v ->
            Bound.entries.flatMap { b -> v.preds(b).map { "$v $b ${it.from} ${it.weight}" } } +
                    "$v synthesized=${(v as? MinVertex)?.isSynthesized} phi=${v is PhiVertex}"
        }.toSet()

        val expected = signature(fc.graph())
        repeat(10) { seed ->
            val shuffled = fc.session.allNodes().toList().shuffled(Random(seed)).asSequence()
            val g = context(fc) { fc.session.withValueTypes { InequalityGraphBuilder.build(shuffled) } }

            assertEquals(expected, signature(g), "seed $seed")
        }
    }
}
