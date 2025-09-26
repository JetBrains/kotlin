package hair.ir.generator

import hair.ir.generator.toolbox.Builtin
import hair.ir.generator.toolbox.Builtin.param
import hair.ir.generator.toolbox.ModelDSL

object ControlFlow : ModelDSL() {

    val goto by node(Builtin.singleExit) {
        //interfaces(blockExit)
    }

    val `if` by node(Builtin.twoExits) {
        param("condition")
    }

    // TODO switch?

    val halt by node(Builtin.noExit)

    val block by abstractClass(Builtin.controlMerge)
    val bBlock by node(block)
    val xBlock by node(block)

    // exceptions

    val `throw` by node(Builtin.throwExit) {
        param("exception")
    }


    // FIXME place near the Phi
    val `catch` by node {
        param("xBlock", xBlock)
        variadicParam("catchedValues")
    }

}