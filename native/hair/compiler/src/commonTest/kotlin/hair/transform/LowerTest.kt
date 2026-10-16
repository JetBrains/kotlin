/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.transform

import hair.ir.*
import hair.ir.nodes.*
import hair.sym.HairType
import hair.test.Cls
import hair.test.Fun
import kotlin.test.*

class LowerTest : IrTest {

    /** `return x as C` */
    @Test
    fun testCheckCastIsRemovedFromControlChain() = withTestSession {
        lateinit var param: Node
        buildInitialIR {
            param = Param(0)
            val cast = CheckCast(Cls("C"))(param)
            Return(cast)
        }
        lower()

        val ret = allNodes<Return>().single()
        assertEquals(param, ret.result)
        assertTrue(allNodes<CheckCast>().none())
    }

    /** `use(x is C, (x as Any?) is C)` */
    @Test
    fun testIsInstanceOfMergedDuringLoweringIsSkipped() = withTestSession {
        val c = Cls("C")
        val use = Fun("use", listOf(HairType.BOOLEAN, HairType.BOOLEAN))
        buildInitialIR {
            val param = Param(0)
            val direct = IsInstanceOf(c)(param)                 // 1 = IsInstanceOf[C](4)
            val cast = CheckCast(c)(param)                      // 2 = CheckCast(…, 4)
            val throughCast = IsInstanceOf(c)(cast)             // 3 = IsInstanceOf[C](2)
            InvokeStatic(use)(callArgs = arrayOf(direct, throughCast))
            ReturnVoid()
        }
        lower()

        assertTrue(allNodes<IsInstanceOf>().none())
        assertTrue(allNodes<CheckCast>().none())
    }
}
