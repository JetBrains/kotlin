package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL

object DataFlow : ModelDSL() {

    val varOp by abstractClass(ControlFlow.blockBody) {
        formParam("variable", Any::class)
    }

    val readVar by node(varOp)

    val assignVar by node(varOp) {
        param("assignedValue")
    }

    val phi by node {
        param("block", ControlFlow.blockEntry)
        variadicParam("joinedValues")
    }

    val param by node {
        formParam("index", Int::class)
    }

    val catch by node {
        param("unwind")
    }
}