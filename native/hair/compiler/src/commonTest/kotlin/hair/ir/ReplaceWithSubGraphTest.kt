package hair.ir

import hair.ir.nodes.*
import hair.sym.CmpOp
import hair.sym.HairType
import hair.sym.RuntimeInterface
import hair.utils.printGraphviz
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplaceWithSubGraphTest : IrTest {

    /** Lowers a spine `ArrayIndexCheck` into an `if (index >=u size) throw` diamond, as in `Lower.kt`. */
    @Test
    fun testLowerArrayIndexCheck() = withTestSession {
        buildInitialIR {
            ArrayIndexCheck(Param(0), Param(1))
            ReturnVoid()
        }

        val check = allNodes<ArrayIndexCheck>().single()
        val array = check.array
        val index = check.index

        check.replaceWithSubGraph {
            val size = ArraySize(array)
            branch(
                cond = Cmp(HairType.INT, CmpOp.U_GE)(index, size),
                trueInit = {
                    InvokeStatic(RuntimeInterface.throwArrayIndexOutOfBounds)()
                },
                falseInit = { },
            )
            null
        }

        verify()
        printGraphviz()

        assertTrue(allNodes<ArrayIndexCheck>().toList().isEmpty(), "the check must be gone")
        assertEquals(1, allNodes<If>().toList().size, "the diamond's If must be present")
        assertTrue(
            allNodes<InvokeStatic>().any { it.function == RuntimeInterface.throwArrayIndexOutOfBounds },
            "the thrower call must be present",
        )
        assertEquals(1, allNodes<ArraySize>().toList().size, "the bound must be read once")
    }
}
