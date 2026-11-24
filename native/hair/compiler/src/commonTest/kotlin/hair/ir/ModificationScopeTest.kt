package hair.ir

import hair.ir.nodes.*
import hair.sym.ArithmeticType
import hair.test.Fun
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModificationScopeTest : IrTest {

    /** A scope is a transaction from one consistent graph to the next, so scopes don't nest. */
    @Test
    fun testScopesDoNotNest() = withTestSession {
        buildInitialIR {
            ReturnVoid()
        }

        val failure = assertFailsWith<IllegalStateException> {
            modifyIR {
                modifyControlFlow { }
            }
        }
        assertTrue(failure.message!!.contains("don't nest"), "unexpected failure: ${failure.message}")

        // A rejected nested scope must not leave the outer one marked as open.
        modifyIR { }
    }

    /**
     * Dead code is removed when the scope ends, and only then: tools like [insertAfter] run within the caller's
     * scope, so they must not sweep away a node the caller has just built and is about to use.
     */
    @Test
    fun testDeadCodeIsEliminatedWhenTheScopeEnds() = withTestSession {
        buildInitialIR {
            ReturnVoid()
        }

        lateinit var pending: Node
        modifyControlFlow {
            pending = Add(ArithmeticType.INT)(Param(0), Param(1))
            entry.insertAfter { InvokeStatic(Fun("f"))(callArgs = arrayOf<Node>()) }
            assertTrue(pending.inGraph, "a tool must not eliminate a node the scope still holds")
        }
        assertFalse(pending.inGraph, "the scope must eliminate the node left unused when it ends")
    }
}
