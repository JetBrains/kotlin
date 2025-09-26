package hair.ir.generator

import hair.ir.generator.toolbox.Builtin
import hair.ir.generator.toolbox.ModelDSL

object Utils : ModelDSL() {

    val noValue by node {}

    val placeholder by node {
        formParam("tag", Any::class)
        variadicParam("inputs")
    }

    val use by node(Builtin.spinal) {
        param("value")
    }

    val proxyProjection by abstractClass {
        interfaces(Builtin.projection)
        param("owner", Builtin.controlFlow)
        param("origin")
    }

}