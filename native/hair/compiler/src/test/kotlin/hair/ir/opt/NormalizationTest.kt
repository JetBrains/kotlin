package hair.ir.opt

import hair.graph.TestGraph
import hair.ir.*
import hair.ir.nodes.Add
import hair.ir.nodes.BinaryOp
import hair.ir.nodes.Node
import hair.ir.nodes.Use
import hair.sym.Type
import kotlin.test.*

class NormalizationTest : IrTest {

    @Test
    fun testConstAdd() = withTestSession {

        buildInitialIR {
            val a = 23L
            val b = 42L
            assertEquals(ConstInt(a + b), Add(Type.Primitive.INT)(ConstInt(a), ConstInt(b)))
        }
    }

    @Test
    fun testConstTree() = withTestSession {
        buildInitialIR {
            val a = 4L
            val b = 8L
            val c = 15L
            val d = 16L
            val e = 23L
            val f = 42L
            assertEquals(
                ConstInt(a + b + c + d + e + f),
                Add(Type.Primitive.INT)(
                    Add(Type.Primitive.INT)(ConstInt(a), ConstInt(b)),
                    Add(Type.Primitive.INT)(
                        Add(Type.Primitive.INT)(ConstInt(c), ConstInt(d)),
                        Add(Type.Primitive.INT)(ConstInt(e), ConstInt(f))
                    )
                )
            )
        }
    }


    @Test
    fun testAfterChange() = withTestSession {
        val a = 23L
        val b = 42L

        lateinit var use: Use
        lateinit var expected: Node

        buildInitialIR {
            use = Use( Add(Type.Primitive.INT)(ConstInt(a), Param(0)) )
            expected = ConstInt(a + b)
        }
        modifyIR {
            (use.value as Add).rhs = ConstInt(b)
        }
        assertEquals(expected, use.value)
    }

}