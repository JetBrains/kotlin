package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL

object Utils : ModelDSL() {

    val noValue by node {}

    val use by node(ControlFlow.blockBody) {
        param("value")
    }

}