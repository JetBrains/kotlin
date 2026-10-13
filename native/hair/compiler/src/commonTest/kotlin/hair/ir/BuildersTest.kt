package hair.ir

import hair.graph.topSort
import hair.ir.nodes.*
import hair.sym.HairClass
import hair.test.Fun
import hair.utils.printGraphviz
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BuildersTest : IrTest {

    @Test
    fun testTryCatch() = withTestSession {
        val f = Fun("f")
        val t1 = object : HairClass {}
        val t2 = object : HairClass {}

        buildInitialIR {
            InvokeStatic(f)(callArgs = arrayOf<Node>())
            tryCatch(
                tryBody = {
                    InvokeStatic(f)(callArgs = arrayOf<Node>())
                    InvokeStatic(f)(callArgs = arrayOf<Node>())
                },
                catches = listOf(
                    t1 to {
                        Use(it)
                    },
                    t2 to {
                        Throw(it)
                    }
                )
            )
            ReturnVoid()
        }

        printGraphviz()
    }

    /**
     * Models `if (p0) use(p1); return 42` — a branching without an unconditional branch.
     * When the condition does not hold, control must fall through to the merge block.
     */
    @Test
    fun testNonExhaustiveBranchFallsThrough() = withTestSession {
        lateinit var ret: Return

        buildInitialIR {
            val cond: ExprBuilder = { Param(0) }
            val body: ExprBuilder = { Use(Param(1)); NoValue() }
            branch(listOf(cond to body))
            ret = Return(Const(42)) as Return
        }

        val merge = ret.control as BlockEntry
        assertEquals(2, merge.preds.size, "both the taken branch and the fall-through must reach the merge block")
        assertTrue(merge.preds.none { it is Unreachable })
        assertTrue(merge in topSort(cfg()), "the merge block must be reachable")
    }

    /**
     * Models `return if (p0) p1 else p2` — the result of an exhaustive branching joins the branch values.
     */
    @Test
    fun testExhaustiveBranchJoinsValues() = withTestSession {
        lateinit var v1: Node
        lateinit var v2: Node
        lateinit var ret: Return

        buildInitialIR {
            val cond: ExprBuilder = { Param(0) }
            val trueBody: ExprBuilder = { Param(1).also { v1 = it } }
            val falseBody: ExprBuilder = { Param(2).also { v2 = it } }
            val result = branch(listOf(cond to trueBody, null to falseBody))
            ret = Return(result) as Return
        }

        val phi = ret.result as Phi
        assertContentEquals(listOf(v1, v2), phi.joinedValues.toList())
    }

    /**
     * Reproduces the shape [hair.ir.tryCatch] sees from IrToHair: the try body is not built by a lambda written
     * inside the `tryCatch` call, but by a long-lived visitor-like object that captured the enclosing builder
     * context earlier. The nodes it builds must still reach `tryCatch`, otherwise no exception edges are created.
     */
    @Test
    fun testTryCatchWithCapturedBuilderContext() = withTestSession {
        val f = Fun("f")
        val t1 = object : HairClass {}

        lateinit var bodyCall: Node

        buildInitialIR {
            val bodyGenerator = object {
                fun generate(): Node = InvokeStatic(f)(callArgs = arrayOf<Node>())
            }

            tryCatch(
                tryBody = { bodyGenerator.generate().also { bodyCall = it } },
                catches = listOf(t1 to { exception -> Use(exception) })
            )
            ReturnVoid()
        }

        val call = bodyCall
        assertIs<Throwing>(call)
        assertNotNull(call.unwind, "the call built through the captured context got no exception edge")
        assertEquals(1, allNodes<Catch>().count())
    }
}
