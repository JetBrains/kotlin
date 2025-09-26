package hair.ir.generator

import hair.ir.generator.toolbox.Builtin
import hair.ir.generator.toolbox.ModelDSL
import hair.sym.HairFunction

object Calls : ModelDSL() {

    val staticCall by node(Builtin.throwingSpinal) {
        interfaces(Object.memoryAccess)
        formParam("function", HairFunction::class)
        param("lastLocationAccess")
        variadicParam("callArgs")
    }

    val `return` by node(Builtin.noExit) {
        interfaces(Object.memoryAccess)
        param("lastLocationAccess")
        param("value")
    }

}