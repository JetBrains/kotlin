/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.test

import hair.ir.*
import hair.ir.nodes.ArrayIndexCheck
import hair.sym.ArithmeticType
import hair.sym.CmpOp
import hair.sym.HairType.*
import kotlin.test.Test
import kotlin.test.assertEquals

class LengthOperandBceTest : BceTest {
    /** `if (3 < a.size) a[3]` */
    @Test
    fun constantGuardedByLengthIsRedundant() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            branch(Cmp(INT, CmpOp.S_LT)(Const(3), ArraySize(a)), { ArrayIndexCheck(a, Const(3)) }, {})
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(0, fc.checks.size)
    }

    /** `if (3 < a.size) a[2]` */
    @Test
    fun smallerConstantUnderGuardIsRedundant() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            branch(Cmp(INT, CmpOp.S_LT)(Const(3), ArraySize(a)), { ArrayIndexCheck(a, Const(2)) }, {})
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(0, fc.checks.size)
    }

    /** `if (3 < a.size) a[4]` */
    @Test
    fun largerConstantUnderGuardIsKept() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            branch(Cmp(INT, CmpOp.S_LT)(Const(3), ArraySize(a)), { ArrayIndexCheck(a, Const(4)) }, {})
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /** `if (3 < a.size) {} else a[3]` */
    @Test
    fun guardDoesNotLeakIntoElseBranch() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            branch(Cmp(INT, CmpOp.S_LT)(Const(3), ArraySize(a)), {}, { ArrayIndexCheck(a, Const(3)) })
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /** `if (a.size < 3) return; a[2]` */
    @Test
    fun earlyReturnGuardCoversTheRest() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            branch(Cmp(INT, CmpOp.S_LT)(ArraySize(a), Const(20)), { ReturnVoid() }, {})
            ArrayIndexCheck(a, Const(2))
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(0, fc.checks.size)
    }

    /** `if (a.size < 3) return; a[3]` */
    @Test
    fun earlyReturnGuardDoesNotCoverItsBound() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            branch(Cmp(INT, CmpOp.S_LT)(ArraySize(a), Const(3)), { ReturnVoid() }, {})
            ArrayIndexCheck(a, Const(3))
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(1, fc.checks.size)
    }

    /** `a[3]; a[1]; a[3]` */
    @Test
    fun smallerConstantAfterLargerIsRedundant() {
        lateinit var first: ArrayIndexCheck
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            first = ArrayIndexCheck(a, Const(3)) as ArrayIndexCheck
            ArrayIndexCheck(a, Const(1))
            ArrayIndexCheck(a, Const(3))
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(listOf(first), fc.checks)
    }

    /** `a[1]; a[3]` */
    @Test
    fun largerConstantAfterSmallerIsKept() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            ArrayIndexCheck(a, Const(1))
            ArrayIndexCheck(a, Const(3))
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(2, fc.checks.size)
    }

    /** `a[3]; b[1]` */
    @Test
    fun checkOfAnotherArrayIsIgnored() {
        val fc = bceIR(REFERENCE, REFERENCE) {
            ArrayIndexCheck(Param(0), Const(3))
            ArrayIndexCheck(Param(1), Const(1))
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(2, fc.checks.size)
    }

    /** `if (c) a[3]; a[1]` */
    @Test
    fun checkInOneBranchDoesNotCoverTheMerge() {
        val fc = bceIR(INT, REFERENCE) {
            val a = Param(1)
            branch(Cmp(INT, CmpOp.S_LT)(Param(0), Const(0)), { ArrayIndexCheck(a, Const(3)) }, {})
            ArrayIndexCheck(a, Const(1))
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(2, fc.checks.size)
    }

    /**
     * ```
     * var i = 0
     * while (i < a.size) {
     *     a[i]
     *     i++
     * }
     * ```
     */
    @Test
    fun loopOverIndicesIsRedundant() {
        val fc = bceIR(REFERENCE) {
            val a = Param(0)
            val i = Var.nextNumbered()

            AssignVar(i)(Const(0))
            whileLoopWithHeader(
                { Cmp(INT, CmpOp.S_LT)(ReadVar(i), ArraySize(a)) },
                {
                    ArrayIndexCheck(a, ReadVar(i))
                    AssignVar(i)(Add(ArithmeticType.INT)(ReadVar(i), Const(1)))
                }
            )
            ReturnVoid()
        }
        fc.runBce()
        assertEquals(0, fc.checks.size)
    }
}
