package hair.ir.generator

import hair.ir.generator.toolbox.Builtin
import hair.ir.generator.toolbox.ModelDSL
import hair.ir.nodes.Var

object DataFlow : ModelDSL() {

    val varOp by abstractClass(Builtin.spinal) {
        formParam("variable", Var::class)
    }

    val readVar by node(varOp)

    val assignVar by node(varOp) {
        param("assignedValue")
    }

    val phi by node {
        param("block", ControlFlow.block)
        variadicParam("joinedValues")
    }

    val param by node {
        formParam("number", Int::class)
    }
}