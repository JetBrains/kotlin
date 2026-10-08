/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.opt

import hair.ir.ArrayIndexCheck
import hair.ir.*
import hair.ir.nodes.ArrayIndexCheck
import hair.sym.ArithmeticType
import hair.sym.CmpOp
import hair.sym.HairType.*
import hair.test.BceTest
import hair.test.Cls
import hair.test.Var
import hair.test.whileLoopWithHeader
import kotlin.test.*

class BoundsCheckEliminationTest : BceTest {
    @Test
    fun repeatedAccessIsRedundant() {
        lateinit var first: ArrayIndexCheck
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val a = Param(1)

            first = ArrayIndexCheck(a, i) as ArrayIndexCheck
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(listOf(first), fc.checks)
    }

    @Test
    fun unguardedAccessIsKept() {
        val fc = bceIR(INT, REFERENCE) {
            ArrayIndexCheck(Param(0), Param(1))
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /** `if (i >= 0) { if (i < a.size) a[i] }` */
    @Test
    fun nestedGuardIsRedundant() {
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val a = Param(1)

            branch(Cmp(INT, CmpOp.S_LT)(i, Const(0)), {}, {
                branch(Cmp(INT, CmpOp.S_LT)(i, ArraySize(a)), { ArrayIndexCheck(a, i) }, {})
            })
            ReturnVoid()
        }
        fc.runBce()
        assertTrue(fc.checks.isEmpty())
    }

    /** `if (i < a.size) a[i]` - the upper bound alone is not enough */
    @Test
    fun upperGuardOnlyIsKept() {
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val a = Param(1)

            branch(Cmp(INT, CmpOp.S_LT)(i, ArraySize(a)), { ArrayIndexCheck(a, i) }, {})
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /** `if (i >= 0) { if (i < a.size) b[i] }` */
    @Test
    fun guardOnOtherArrayIsKept() {
        val fc = bceIR(INT, REFERENCE, REFERENCE) {
            val i = Param(0)
            val a = Param(1)
            val b = Param(2)

            branch(Cmp(INT, CmpOp.S_LT)(i, Const(0)), {}, {
                branch(Cmp(INT, CmpOp.S_LT)(i, ArraySize(a)), { ArrayIndexCheck(b, i) }, {})
            })
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /** `val a = IntArray(10); a[9]; a[10]` - Not provable by current CFG design */
    @Test
    fun constantIndexIntoConstantSizedArray() {
        lateinit var outOfBounds: ArrayIndexCheck
        val fc = bceIR {
            val a = NewArray(Cls("IntArray"))(Const(10))
            ArrayIndexCheck(a, Const(9))
            outOfBounds = ArrayIndexCheck(a, Const(10)) as ArrayIndexCheck
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(listOf(outOfBounds), fc.checks)
    }

    /** `var i = 0; while (i < a.size) { a[i]; i++ }` */
    @Test
    fun loopOverIndicesIsRedundant() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            val i = Var.nextNumbered()
            AssignVar(i)(Const(0))
            whileLoopWithHeader({ Cmp(INT, CmpOp.S_LT)(ReadVar(i), ArraySize(a)) }) {
                ArrayIndexCheck(a, ReadVar(i))
                AssignVar(i)(Add(ArithmeticType.INT)(ReadVar(i), Const(1)))
            }
            ReturnVoid()
        }
        fc.runBce()
        assertTrue(fc.checks.isEmpty())
    }

    /** `var i = 0; while (i <= a.size) { a[i]; i++ }` - off by one */
    @Test
    fun offByOneLoopIsKept() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            val i = Var.nextNumbered()
            AssignVar(i)(Const(0))
            whileLoopWithHeader({ Cmp(INT, CmpOp.S_LE)(ReadVar(i), ArraySize(a)) }) {
                ArrayIndexCheck(a, ReadVar(i))
                AssignVar(i)(Add(ArithmeticType.INT)(ReadVar(i), Const(1)))
            }
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /** `if (i >= 0) { val k = i + 1; if (k < a.size) a[k] }` = i = MAX_VALUE makes k negative */
    @Test
    fun incrementThatCanWrapIsKept() {
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val a = Param(1)

            branch(Cmp(INT, CmpOp.S_LT)(i, Const(0)), {}, {
                val k = Add(ArithmeticType.INT)(i, Const(1))
                branch(Cmp(INT, CmpOp.S_LT)(k, ArraySize(a)), { ArrayIndexCheck(a, k) }, {})
            })
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /** `if (i >= 0) { if (i < a.size - 1) a[i + 1] }` */
    @Test
    fun incrementBoundedByLengthIsRedundant() {
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val a = Param(1)

            branch(Cmp(INT, CmpOp.S_LT)(i, Const(0)), {}, {
                val last = Add(ArithmeticType.INT)(ArraySize(a), Const(-1))
                branch(Cmp(INT, CmpOp.S_LT)(i, last), {
                    ArrayIndexCheck(a, Add(ArithmeticType.INT)(i, Const(1)))
                }, {})
            })
            ReturnVoid()
        }
        fc.runBce()
        assertTrue(fc.checks.isEmpty())
    }

    /**
     * Bidirectional bubble sort
     * ```
     * var limit = a.size
     * var st = -1
     * while (st < limit)
     *     st++
     *     limit--
     *     var j = st
     *     while (j < limit) { a[j]; j++ }
     * }
     * ```
     */
    @Test
    fun shrinkingWindowIsRedundant() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            val limit = Var.nextNumbered()
            val st = Var.nextNumbered()
            val j = Var.nextNumbered()

            AssignVar(limit)(ArraySize(a))
            AssignVar(st)(Const(-1))
            whileLoopWithHeader({ Cmp(INT, CmpOp.S_LT)(ReadVar(st), ReadVar(limit)) }) {
                AssignVar(st)(Add(ArithmeticType.INT)(ReadVar(st), Const(1)))
                AssignVar(limit)(Add(ArithmeticType.INT)(ReadVar(limit), Const(-1)))
                AssignVar(j)(ReadVar(st))
                whileLoopWithHeader({ Cmp(INT, CmpOp.S_LT)(ReadVar(j), ReadVar(limit)) }) {
                    ArrayIndexCheck(a, ReadVar(j))
                    AssignVar(j)(Add(ArithmeticType.INT)(ReadVar(j), Const(1)))
                }
            }
            ReturnVoid()
        }
        fc.runBce()
        assertTrue(fc.checks.isEmpty())
    }

    /**
     * ```
     * var a = Param(0)
     * var i = 0
     * while (i < a.size) {
     *     a = IntArray(1)
     *     a[i]
     *     i++
     * }
     * ```
     */
    @Test
    fun reassignedArraySizeChangeKeepsCheck() {
        val fc = bceIR(REFERENCE) {
            val initialArray = Param(0)
            val aVar = Var.nextNumbered()
            val iVar = Var.nextNumbered()

            AssignVar(aVar)(initialArray)
            AssignVar(iVar)(Const(0))
            whileLoopWithHeader({
                                    Cmp(INT, CmpOp.S_LT)(ReadVar(iVar), ArraySize(ReadVar(aVar)))
                                }) {
                val newArr = NewArray(Cls("IntArray"))(Const(1))
                AssignVar(aVar)(newArr)

                ArrayIndexCheck(ReadVar(aVar), ReadVar(iVar))

                AssignVar(iVar)(Add(ArithmeticType.INT)(ReadVar(iVar), Const(1)))
            }
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /**
     * ```
     * var a = Param(0)
     * if (i >= 0 && i < a.size) {
     *     a = IntArray(2)
     *     a[i]
     * }
     * ```
     */
    @Test
    fun branchReassignmentKeepsCheck() {
        val fc = bceIR(INT, REFERENCE) {
            val i = Param(0)
            val initialArray = Param(1)
            val aVar = Var.nextNumbered()

            AssignVar(aVar)(initialArray)
            branch(Cmp(INT, CmpOp.S_LT)(i, Const(0)), {}, {
                branch(Cmp(INT, CmpOp.S_LT)(i, ArraySize(ReadVar(aVar))), {
                    val resized = NewArray(Cls("IntArray"))(Const(2))
                    AssignVar(aVar)(resized)
                    ArrayIndexCheck(ReadVar(aVar), i)
                }, {})
            })
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }
}
