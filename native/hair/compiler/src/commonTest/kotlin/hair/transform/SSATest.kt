package hair.transform

import hair.ir.*
import hair.ir.Add
import hair.ir.nodes.AssignVar
import hair.ir.nodes.NodeBuilder
import hair.test.Var
import hair.ir.nodes.Phi
import hair.ir.nodes.ReadVar
import hair.ir.nodes.Throw
import hair.sym.HairFunction
import hair.sym.HairType.*
import hair.test.Fun
import hair.utils.printGraphviz
import kotlin.test.*

class SSATest : IrTest {

    context(_: NodeBuilder)
    private fun cond() = Param(1010)

    @Test
    fun testIf() = withTestSession {
        buildInitialIR {
            val v1 = ConstI(23)
            val v2 = ConstI(42)
            val v = Var.nextNumbered()
            branch(cond(), {
                AssignVar(v)(v1)
            }, {
                AssignVar(v)(v2)
            })
            val use = Use(ReadVar(v))

            ReturnVoid()

            buildSSA { INT }

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
            AssignVar(var0)(ConstI(0))
            branch(cond(), {
                AssignVar(var0)(ConstI(10))
                AssignVar(var1)(ConstI(21))
            }, {
                AssignVar(var1)(ConstI(22))
                AssignVar(var0)(ConstI(20))
            })
            AssignVar(var0)(Add(INT)(ReadVar(var0), ConstI(1)))
            AssignVar(var2)(ConstI(0))
            branch(ReadVar(var1), {
                whileLoop(cond()) {
                    AssignVar(var2)(Add(INT)(ReadVar(var2), ConstI(1)))
                }
            }, {
                AssignVar(var2)(ReadVar(var1))
            })
            whileLoop(ReadVar(var2)) {}

            ReturnVoid()

            buildSSA { INT }

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
                Use(ConstI(23))
            }, {
                branch(cond(), {
                    Use(Add(INT)(ConstI(23), ConstI(42)))
                }, {
                    whileLoop(cond()) {
                        Use(Add(INT)(ConstI(23), ConstI(42)))
                    }
                })
            })

            ReturnVoid()

            buildSSA { INT }

            printGraphviz()
        }
    }

    @Test
    fun testTwoThrowsSameBlock() = withTestSession {
        buildInitialIR {
            val v = Var.nextNumbered()

            val v1 = ConstI(23)
            AssignVar(v)(v1)

            val call = InvokeStatic(Fun("foo"))()

            val v2 = ConstI(42)
            AssignVar(v)(v2)

            val thr = Throw(ConstI(108))

            val callUnwind = Unwind(call)
            val throwUnwind = Unwind(thr)

            BlockEntry(callUnwind, throwUnwind)
            val ret = Return(ReadVar(v))

            buildSSA { INT }

            printGraphviz()

            val result = ret.result
            assertTrue(result is Phi)
            assertContentEquals(listOf(v1, v2), result.joinedValues)
        }
    }

    @Test
    fun testTryCatchComplex() = withTestSession {
        buildInitialIR {
            val v = Var.nextNumbered()

            val v1 = ConstI(23)
            AssignVar(v)(v1)

            val call = InvokeStatic(Fun("foo"))()

            val v2 = ConstI(42)
            AssignVar(v)(v2)

            lateinit var thr: Throw
            val v3 = ConstI(37)
            branch(
                cond(),
                {
                    AssignVar(v)(v3)
                },
                {
                    thr = Throw(ConstI(108))
                }
            )

            val retNormal = Return(ReadVar(v))

            val callUnwind = Unwind(call)
            val throwUnwind = Unwind(thr)

            BlockEntry(callUnwind, throwUnwind)
            val retHandler = Return(ReadVar(v))

            buildSSA { INT }

            printGraphviz()

            assertContentEquals(listOf(v1, v2), (retHandler.result as Phi).joinedValues)
            assertEquals(v3, retNormal.result)
        }
    }

}