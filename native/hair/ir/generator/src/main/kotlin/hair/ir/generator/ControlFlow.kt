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

    val unreachable by node {
        interfaces(controlling, blockExit)
    }

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
        param("cond")
    }

    val ifProjection by abstractClass {
        interfaces(projection)
        interfaces(blockExit)
        param("owner", `if`)
    }

    val TrueExit by node(ifProjection)
    val FalseExit by node(ifProjection)

    val `throw` by node(blockEnd) {
        interfaces(throwing)
        param("exception")
    }

    val unwind by node {
        interfaces(blockExit)
        param("thrower", throwing)
    }
}