package hair.ir.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.opt.inline
import hair.sym.HairFunction
import hair.utils.printGraphviz
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InlineTest : AbstractInlinerTest {

    data class Fun(val name: String) : HairFunction

    @Test
    fun testSimpleReturn() = inlinerTest {
        val callee = define(Fun("callee")) {
            Return(Param(0))
        }

        lateinit var call: InvokeStatic
        val caller = define(Fun("caller")) {
            // FIXME varargs :c
            call = InvokeStatic(callee.function)(callArgs = arrayOf(ConstI(42)))
            Use(call)
            ReturnVoid()
        }

        compilation.run {
            inline(call)

            caller.session.run {
                printGraphviz()
                val use = allNodes<Use>().single()
                modifyIR {
                    assertEquals(ConstI(42), use.value)
                }
            }
        }
    }

    @Test
    fun testTwoReturns() = inlinerTest {
        val callee = define(Fun("callee")) {
            branch(Param(0), {
                Return(Param(1))
            }, {
                Return(Param(2))
            })
        }

        lateinit var call: InvokeStatic
        val caller = define(Fun("caller")) {
            // FIXME varargs :c
            call = InvokeStatic(callee.function)(callArgs = arrayOf(ConstI(1), ConstI(2), ConstI(3)))
            Use(call)
            ReturnVoid()
        }

        compilation.run {
            inline(call)

            call.session.allNodes().forEach { println(it) }

            caller.session.run {
                printGraphviz()
                val use = allNodes<Use>().single()
                modifyIR {
                    val result = use.value
                    assertTrue(result is Phi)
                    assertContentEquals(listOf(ConstI(2), ConstI(3)), result.joinedValues)
                }
            }
        }
    }

    // TODO test memory chains after inline
}