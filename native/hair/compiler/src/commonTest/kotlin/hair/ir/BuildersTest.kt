package hair.ir

import hair.ir.nodes.Node
import hair.sym.HairClass
import hair.sym.HairFunction
import hair.sym.HairType
import hair.sym.HairType.*
import hair.sym.Type
import hair.test.Fun
import hair.utils.printGraphviz
import kotlin.test.Test

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
}