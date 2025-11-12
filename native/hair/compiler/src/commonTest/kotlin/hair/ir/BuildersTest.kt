package hair.ir

import hair.ir.nodes.Node
import hair.sym.HairFunction
import hair.sym.Type
import hair.utils.printGraphviz
import kotlin.test.Test

class BuildersTest : IrTest {

    @Test
    fun testTryCatch() = withTestSession {
        val f = object : HairFunction {}
        val t1 = object : Type.Reference {}
        val t2 = object : Type.Reference {}

        buildInitialIR {
            StaticCall(f)(callArgs = arrayOf<Node>())
            tryCatch(
                tryBody = {
                    StaticCall(f)(callArgs = arrayOf<Node>())
                    StaticCall(f)(callArgs = arrayOf<Node>())
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