package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL

object Utils : ModelDSL() {

    val noValue by node {}

    // TODO move to Kotlin?
    val unitValue by node {}

    // used in tests
    val use by node(ControlFlow.blockBody) {
        param("value")
    }

}