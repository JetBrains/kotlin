package hair.ir.opt

import hair.ir.*
import hair.ir.Add
import hair.ir.nodes.Add
import hair.ir.nodes.ConstI
import hair.ir.nodes.Node
import hair.ir.nodes.Return
import hair.ir.nodes.Throw
import hair.ir.nodes.Use
import hair.sym.HairType
import hair.sym.HairType.*
import kotlin.test.*

class NormalizationTest : IrTest {

    // TODO Phi normalization

    @Test
    fun testConstAdd() = withTestSession {

        buildInitialIR {
            val a = 23
            val b = 42
            assertEquals(ConstI(a + b), Add(INT)(ConstI(a), ConstI(b)))
        }
    }

    @Test
    fun testConstTree() = withTestSession {
        buildInitialIR {
            val a = 4
            val b = 8
            val c = 15
            val d = 16
            val e = 23
            val f = 42
            assertEquals(
                ConstI(a + b + c + d + e + f),
                Add(INT)(
                    Add(INT)(ConstI(a), ConstI(b)),
                    Add(INT)(
                        Add(INT)(ConstI(c), ConstI(d)),
                        Add(INT)(ConstI(e), ConstI(f))
                    )
                )
            )
        }
    }


    @Test
    fun testAfterChange() = withTestSession {
        val a = 23
        val b = 42

        lateinit var use: Use
        lateinit var expected: Node

        buildInitialIR {
            use = Use(Add(INT)(ConstI(a), Param(0))) as Use
            expected = ConstI(a + b)
            Return(expected)
        }
        modifyIR {
            (use.value as Add).rhs = ConstI(b)
        }
        assertEquals(expected, use.value)
    }


    @Ignore // FIXME decide how Catch should be implemented and normalized
    @Test
    fun testCatchOfThrow() = withTestSession {
        val value = 42

        buildInitialIR {
            val thr = Throw(ConstI(value)) as Throw
            val unwind = Unwind(thr)
            BlockEntry(unwind)
            val catch = Catch(unwind)
            Return(catch)
        }
        val ret = allNodes<Return>().single()
        assertEquals(value, (ret.result as ConstI).value)
    }

}