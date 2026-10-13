package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL

object DataFlow : ModelDSL() {

    val valueNode by nodeInterface()

    val varOp by abstractClass(ControlFlow.blockBody) {
        formParam("variable", Any::class)
    }

    val readVar by node(varOp) {
        interfaces(valueNode)
    }

    val assignVar by node(varOp) {
        param("assignedValue")
    }

    val phi by node {
        interfaces(valueNode)
        param("block", ControlFlow.blockEntry)
        variadicParam("joinedValues")
    }

    val phiPlaceholder by node {
        interfaces(valueNode)
        formParam("origin", Any::class)
        param("block", ControlFlow.blockEntry)
        variadicParam("joinedValues")
    }

    val param by node {
        interfaces(valueNode)
        formParam("index", Int::class)
    }

    val catch by node {
        param("unwind")
    }
}
