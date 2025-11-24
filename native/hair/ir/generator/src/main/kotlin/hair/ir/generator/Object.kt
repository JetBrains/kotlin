package hair.ir.generator

import hair.ir.generator.toolbox.*
import hair.sym.*

object Object : ModelDSL() {

    // new

    val anyNew by nodeInterface()

    val new by node(ControlFlow.blockBody) {
        interfaces(anyNew)
        formParam("objectType", Class::class)
    }

    // type-checks

    val typeCheck by abstractClass {
        formParam("targetType", Type.Reference::class)
        param("obj")
    }

    val isInstanceOf by node(typeCheck)

    val checkCast by node(typeCheck)

}