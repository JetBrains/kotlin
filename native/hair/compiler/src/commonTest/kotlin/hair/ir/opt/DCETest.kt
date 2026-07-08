package hair.ir.opt

import hair.ir.*
import hair.ir.nodes.*
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

    @Test
    fun testDeadPhiCycle() = withTestSession {
        lateinit var mergeBlock: BlockEntry
        buildInitialIR {
            branch(Param(0), {}, {})
            mergeBlock = contextOf<ControlFlowBuilder>().lastControl as BlockEntry
            ReturnVoid()
        }

        modifyIR {
            // Two phis that reference only each other. Each also gets a distinct constant so that
            // normalization doesn't collapse it (a phi with a single distinct input folds away).
            // Nothing else uses them, so together they form a dead reference cycle: the naive
            // "no remaining uses" DCE could never remove them, since each keeps the other alive.
            val phi1 = Phi(mergeBlock, Const(1), Const(2)) as Phi
            val phi2 = Phi(mergeBlock, Const(3), phi1) as Phi
            phi1.joinedValues[0] = phi2
        }

        assertTrue(allNodes<Phi>().none(), "dead phi cycle should have been eliminated")
    }
}
