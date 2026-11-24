package hair.ir.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.opt.inline
import hair.test.Fun
import hair.utils.printGraphviz
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InlineTest : AbstractInlinerTest {

    @Test
    fun testSimpleReturn() = inlinerTest {
        val callee = define(Fun("callee")) {
            Return(Param(0))
        }

        lateinit var call: InvokeStatic
        val caller = define(Fun("caller")) {
            // FIXME varargs :c
            call = InvokeStatic(callee.function)(callArgs = arrayOf(Const(42))) as InvokeStatic
            Use(call)
            ReturnVoid()
        }

        compilation.run {
            inline(call)

            caller.session.run {
                printGraphviz()
                val use = allNodes<Use>().single()
                modifyIR {
                    assertEquals(Const(42), use.value)
                }
            }
        }
    }

    /** A callee whose returns carry no value: the call is still replaced and the spine stays connected. */
    @Test
    fun testReturnWithoutValue() = inlinerTest {
        val callee = define(Fun("callee")) {
            Return(null)
        }

        lateinit var call: InvokeStatic
        lateinit var ret: Return
        val caller = define(Fun("caller")) {
            call = InvokeStatic(callee.function)(callArgs = arrayOf<Node>()) as InvokeStatic
            ret = ReturnVoid() as Return
        }

        compilation.run {
            inline(call)

            caller.session.run {
                assertTrue(allNodes<InvokeStatic>().none(), "the call must be replaced")
                assertTrue(ret.control is BlockEntry, "the caller must continue in the block joining the returns")
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
            call = InvokeStatic(callee.function)(callArgs = arrayOf(Const(1), Const(2), Const(3))) as InvokeStatic
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
                    assertContentEquals(listOf(Const(2), Const(3)), result.joinedValues)
                }
            }
        }
    }

    // TODO test memory chains after inline
}
