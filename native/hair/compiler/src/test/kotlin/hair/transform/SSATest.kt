package hair.transform

import hair.ir.*
import hair.ir.nodes.AssignVar
import hair.ir.nodes.Var
import hair.ir.nodes.Phi
import hair.ir.nodes.ReadVar
import hair.ir.nodes.Throw
import hair.sym.HairFunction
import hair.sym.Type
import hair.utils.printGraphviz
import kotlin.test.*

class SSATest : IrTest {

    // FIXME introduce shorter name for the main builder
    private fun NodeBuilder.cond() = Param(1010)

    @Test
    fun testIf() = withTestSession {
        buildInitialIR {
            val v1 = ConstInt(23)
            val v2 = ConstInt(42)
            val v = Var.nextNumbered()
            branch(cond(), {
                AssignVar(v)(v1)
            }, {
                AssignVar(v)(v2)
            })
            val use = Use(ReadVar(v))

            Halt()

            buildSSA()

            printGraphviz()

            val result = use.value
            assertTrue(result is Phi)
            assertContentEquals(listOf(v1, v2), result.joinedValues)
            assertTrue(allNodes<AssignVar>().toList().isEmpty())
            assertTrue(allNodes<ReadVar>().toList().isEmpty())
        }
    }

    @Test
    fun testComplex1() = withTestSession {
        buildInitialIR {
            val var0 = Var(0)
            val var1 = Var(1)
            val var2 = Var(2)
            AssignVar(var0)(ConstInt(0))
            branch(cond(), {
                AssignVar(var0)(ConstInt(10))
                AssignVar(var1)(ConstInt(21))
            }, {
                AssignVar(var1)(ConstInt(22))
                AssignVar(var0)(ConstInt(20))
            })
            AssignVar(var0)(Add(Type.Primitive.INT)(ReadVar(var0), ConstInt(1)))
            AssignVar(var2)(ConstInt(0))
            branch(ReadVar(var1), {
                whileLoop(cond()) {
                    AssignVar(var2)(Add(Type.Primitive.INT)(ReadVar(var2), ConstInt(1)))
                }
            }, {
                AssignVar(var2)(ReadVar(var1))
            })
            whileLoop(ReadVar(var2)) {}

            Halt()

            buildSSA()

            printGraphviz()

            // TODO assert more?
            assertTrue(allNodes<AssignVar>().toList().isEmpty())
            assertTrue(allNodes<ReadVar>().toList().isEmpty())
        }
    }

    // FIXME seems to be more of a test add normalization
    @Test
    fun testComplex2() = withTestSession {
        buildInitialIR {
            branch(cond(), {
                Use(ConstInt(23))
            }, {
                branch(cond(), {
                    Use(Add(Type.Primitive.INT)(ConstInt(23), ConstInt(42)))
                }, {
                    whileLoop(cond()) {
                        Use(Add(Type.Primitive.INT)(ConstInt(23), ConstInt(42)))
                    }
                })
            })

            Halt()
            printGraphviz()

            buildSSA()

            printGraphviz()
        }
    }

    @Test
    fun testTryCatch() = withTestSession {
        data class Fun(val name: String) : HairFunction

        buildInitialIR {
            val v = Var.nextNumbered()

            AssignVar(v)(ConstInt(23))

            val call = StaticCall(Fun("foo"))()

            AssignVar(v)(ConstInt(42))

            lateinit var thr: Throw
            thr = Throw(ConstInt(108))
//            branch(
//                cond(),
//                {
//                    AssignVar(v)(ConstInt(37))
//                },
//                {
//                    thr = Throw(ConstInt(108))
//                }
//            )

//            Use(ReadVar(v))
//
//            Halt()

            val handler = CatchBlock()
            call.handler = handler
            thr.handler = handler

            Use(ReadVar(v))

            Halt()

            //printGraphviz()

            buildSSA()

            printGraphviz()
        }
    }
}