package hair.ir.opt

import hair.ir.*
import hair.opt.optimize
import hair.test.Fun
import hair.utils.printGraphviz
import kotlin.test.*

class DCETest : IrTest {

    @Test
    fun testAfterInitialIR() = withTestSession {
        buildInitialIR {
            ReturnVoid()
            BlockEntry()
            branch(Param(1010), {
                whileLoop(Param(1010), {
                    Use(Const(42))
                })
            }, {
                tryCatch(
                    {
                        val f = InvokeStatic(Fun("f"))()
                        val o = InvokeStatic(Fun("o"))(callArgs = arrayOf(f))
                        val o2 = InvokeStatic(Fun("o"))(callArgs = arrayOf(o, f))
                        ReturnVoid()
                    },
                    emptyList()
                )
                BlockEntry()
                Use(Const(37))
            })
        }
        // FIXME maybe just DCE?
        optimize()
        printGraphviz()
    }
}
