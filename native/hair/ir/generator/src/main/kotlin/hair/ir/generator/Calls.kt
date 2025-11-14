package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL
import hair.sym.HairFunction

object Calls : ModelDSL() {

    val anyCall by nodeInterface {
    }

    val anyInvoke by abstractClass(ControlFlow.blockBodyWithException) {
        interfaces(anyCall)
        formParam("function", HairFunction::class)
        variadicParam("callArgs")
    }

    val invokeStatic by node(anyInvoke)
    val invokeVirtual by node(anyInvoke)

}