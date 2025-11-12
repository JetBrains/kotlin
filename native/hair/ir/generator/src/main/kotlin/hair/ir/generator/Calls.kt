package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL
import hair.sym.HairFunction

object Calls : ModelDSL() {

    val staticCall by node(ControlFlow.blockBodyWithException) {
        formParam("function", HairFunction::class)
        variadicParam("callArgs")
    }

}