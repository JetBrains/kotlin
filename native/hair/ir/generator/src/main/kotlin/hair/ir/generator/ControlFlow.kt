package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL

object ControlFlow : ModelDSL() {

    val controlFlow by nodeInterface()

    val projection by nodeInterface(controlFlow) {
        param("owner", controlFlow)
    }

    val controlling by nodeInterface(controlFlow)
    val throwing by nodeInterface(controlFlow)
    val blockExit by nodeInterface(controlFlow)

    val blockEntry by node {
        interfaces(controlling)
        variadicParam("preds", blockExit)
    }

    val controlled by abstractClass {
        interfaces(controlFlow)
        param("control", controlling)
    }

    val blockBody by abstractClass(controlled) {
        interfaces(controlling)
    }

    val blockBodyWithException by abstractClass(blockBody) {
        interfaces(throwing)
    }

    // TODO last location access?
    val blockEnd by abstractClass(controlled)

    val `return` by node(blockEnd) {
        param("result")
    }

    val goto by node(blockEnd) {
        interfaces(blockExit)
    }

    val `if` by node(blockEnd) {
        val ifNode = this
        val ifProjection by abstractClass {
            interfaces(projection)
            interfaces(blockExit)
            param("owner", ifNode)
        }
        nestedProjection("trueExit", "True", ifProjection)
        nestedProjection("falseExit", "False", ifProjection)
        param("cond")
    }

    val `throw` by node(blockEnd) {
        interfaces(throwing)
        param("exception")
    }

    val unwind by node {
        interfaces(blockExit)
        param("thrower", throwing)
    }
}