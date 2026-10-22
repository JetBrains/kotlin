package hair.ir.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.opt.optimize
import hair.sym.HairType
import hair.test.Cls
import hair.test.Fld
import hair.test.Var
import hair.transform.buildSSA
import hair.utils.printGraphviz
import kotlin.test.*

class SimplificationTest : IrTest {

    @Test
    fun testIfTrue() = withTestSession {
        buildInitialIR {
            val v = Var.nextNumbered()
            branch(
                True(),
                { AssignVar(v)(Const(42)) },
                { AssignVar(v)(Const(37)) }
            )
            Return(ReadVar(v))
        }
        buildSSA()
        optimize()
        printGraphviz()
        val ret = allNodes<Return>().single()
        assertEquals(42, (ret.result as Const).value)
    }

    // if (true) v = 42 else v = obj.value; return v
    @Test
    fun testIfTrueWithControlledValueInDeadBranch() = withTestSession {
        val field = Fld("value", HairType.INT, Cls("C"))
        buildInitialIR {
            val v = Var.nextNumbered()
            val obj = Param(0)
            branch(
                True(),
                { AssignVar(v)(Const(42)) },
                { AssignVar(v)(LoadField(field)(obj)) },   // controlled value in the dead branch
            )
            Return(ReadVar(v))
        }
        buildSSA()
        optimize()

        val ret = allNodes<Return>().single()
        assertEquals(42, (ret.result as Const).value)
    }
}
