package hair.ir.opt

import hair.ir.*
import hair.ir.Add
import hair.ir.nodes.*
import hair.opt.optimize
import hair.opt.simplify
import hair.sym.HairType
import hair.sym.HairType.*
import hair.test.Fun
import hair.test.Var
import hair.transform.buildSSA
import hair.utils.printGraphviz
import hair.utils.printGraphvizNoGCM
import kotlin.test.*

class SimplificationTest : IrTest {

    @Test
    fun testIfTrue() = withTestSession {
        buildInitialIR {
            val v = Var.nextNumbered()
            branch(
                True(),
                { AssignVar(v)(ConstI(42)) },
                { AssignVar(v)(ConstI(37)) }
            )
            Return(ReadVar(v))
        }
        buildSSA { INT }
        optimize()
        printGraphviz()
        val ret = allNodes<Return>().single()
        assertEquals(42, (ret.result as ConstI).value)
    }
}
