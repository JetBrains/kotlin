package hair.ir.opt

import hair.ir.modifyIR
import hair.ir.nodes.Phi
import hair.ir.nodes.StaticCall
import hair.ir.nodes.Use
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

        lateinit var call: StaticCall
        val caller = define(Fun("caller")) {
            // FIXME varargs :c
            call = StaticCall(callee.function)(callArgs = arrayOf(ConstInt(42)))
            Use(call)
            Halt()
        }

        compilation.run {
            inline(call)

            caller.session.run {
                printGraphviz()
                val use = allNodes<Use>().single()
                modifyIR {
                    assertEquals(ConstInt(42), use.value)
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

        lateinit var call: StaticCall
        val caller = define(Fun("caller")) {
            // FIXME varargs :c
            call = StaticCall(callee.function)(callArgs = arrayOf(ConstInt(1), ConstInt(2), ConstInt(3)))
            Use(call)
            Halt()
        }

        compilation.run {
            inline(call)

            caller.session.run {
                printGraphviz()
                val use = allNodes<Use>().single()
                modifyIR {
                    val result = use.value
                    assertTrue(result is Phi)
                    assertContentEquals(listOf(ConstInt(2), ConstInt(3)), result.joinedValues)
                }
            }
        }
    }

    // TODO test memory chains after inline
}