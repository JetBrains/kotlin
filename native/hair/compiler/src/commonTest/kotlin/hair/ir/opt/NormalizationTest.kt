package hair.ir.opt

import hair.ir.*
import hair.ir.nodes.AddI
import hair.ir.nodes.ConstI
import hair.ir.nodes.Node
import hair.ir.nodes.Return
import hair.ir.nodes.Use
import kotlin.test.*

class NormalizationTest : IrTest {

    // TODO Phi normalization

    @Test
    fun testConstAdd() = withTestSession {

        buildInitialIR {
            val a = 23
            val b = 42
            assertEquals(ConstI(a + b), AddI(ConstI(a), ConstI(b)))
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
                AddI(
                    AddI(ConstI(a), ConstI(b)),
                    AddI(
                        AddI(ConstI(c), ConstI(d)),
                        AddI(ConstI(e), ConstI(f))
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
            use = Use( AddI(ConstI(a), Param(0)) )
            expected = ConstI(a + b)
            Return(expected)
        }
        modifyIR {
            (use.value as AddI).rhs = ConstI(b)
        }
        assertEquals(expected, use.value)
    }


    @Ignore // FIXME decide how Catch should be implemented and normalized
    @Test
    fun testCatchOfThrow() = withTestSession {
        val value = 42

        buildInitialIR {
            val thr = Throw(ConstI(value))
            val unwind = Unwind(thr)
            BlockEntry(unwind)
            val catch = Catch(unwind)
            Return(catch)
        }
        val ret = allNodes<Return>().single()
        assertEquals(value, (ret.result as ConstI).value)
    }

}