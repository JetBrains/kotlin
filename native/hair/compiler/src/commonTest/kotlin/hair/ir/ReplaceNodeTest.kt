package hair.ir

import hair.ir.nodes.*
import hair.sym.HairClass
import hair.test.Fun
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReplaceNodeTest : IrTest {

    /** Builds `try { f(); f() } catch (t1) { }`, so the calls in the try body carry exception edges. */
    private fun Session.buildTryCatch(f: Fun, catchType: HairClass) {
        buildInitialIR {
            tryCatch(
                tryBody = {
                    InvokeStatic(f)(callArgs = arrayOf<Node>())
                    InvokeStatic(f)(callArgs = arrayOf<Node>())
                },
                catches = listOf(catchType to { NoValue() }),
            )
            ReturnVoid()
        }
    }

    /**
     * A thrower's exception edge is an `Unwind` *use* of it, and `Unwind` is not `Controlled`, so the plain
     * value-use walk would happily re-point `Unwind.thrower` at a replacement that cannot throw at all.
     * [replaceValueNodeWith] must refuse instead.
     */
    @Test
    fun testReplaceWithRefusesAThrower() = withTestSession {
        val f = Fun("f")
        buildTryCatch(f, object : HairClass {})

        val thrower = allNodes<InvokeStatic>().first { it.unwind != null }

        val failure = assertFailsWith<IllegalArgumentException> {
            modifyIR {
                thrower.replaceWith(thrower.control, NoValue())
            }
        }
        assertTrue(
            failure.message!!.contains("replaceThrowing"),
            "the refusal must name the operation to use instead, was: ${failure.message}",
        )
    }

    /** [replaceThrowing] is the explicit form: it carries the exception edge over to the replacement. */
    @Test
    fun testReplaceThrowingCarriesTheExceptionEdge() = withTestSession {
        val f = Fun("f")
        val g = Fun("g")
        buildTryCatch(f, object : HairClass {})

        val original = allNodes<InvokeStatic>().first { it.unwind != null }
        val handlerBefore = original.unwind!!.handler

        modifyIR {
            val replacement = InvokeStatic(g)(original.control, callArgs = arrayOf<Node>()) as InvokeStatic
            original.replaceThrowing(replacement)
        }

        verify()

        val replacement = allNodes<InvokeStatic>().single { it.function == g }
        assertTrue(!original.inGraph, "the replaced call must be gone")
        assertSame(handlerBefore, replacement.unwind?.handler, "the handler must still be reachable")
        assertEquals(1, allNodes<Catch>().count(), "the catch must survive")
    }

    /** [spliceOut] joins predecessor to successor and redirects the value uses in one step. */
    @Test
    fun testSpliceOut() = withTestSession {
        buildInitialIR {
            val cast = CheckCast(object : HairClass {})(Param(0))
            Return(cast)
        }

        val cast = allNodes<CheckCast>().single()
        val obj = cast.obj
        val predecessor = cast.control

        modifyIR {
            cast.spliceOut(obj)
        }

        verify()

        assertTrue(allNodes<CheckCast>().toList().isEmpty(), "the cast must be gone")
        val ret = allNodes<Return>().single()
        assertSame(obj, ret.result, "the value use must be redirected to the object")
        assertSame(predecessor, ret.control, "the spine must be joined across the removed node")
    }

    /** A node off the control spine takes the value-only op; asking for it on a spine node is refused. */
    @Test
    fun testReplaceWithRejectsControlFlow() = withTestSession {
        buildInitialIR {
            InvokeStatic(Fun("f"))(callArgs = arrayOf<Node>())
            ReturnVoid()
        }

        val call = allNodes<InvokeStatic>().single()
        assertNull(call.unwind, "no exception edge in this fixture")

        val failure = assertFailsWith<IllegalArgumentException> {
            modifyIR {
                (call as Node).replaceValueNodeWith(NoValue())
            }
        }
        assertTrue(
            failure.message!!.contains("control flow"),
            "the refusal must explain that a spine node needs a control replacement, was: ${failure.message}",
        )
    }
}
