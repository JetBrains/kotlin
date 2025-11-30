package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL

object Utils : ModelDSL() {

    // used in tests
    val use by node(ControlFlow.blockBody) {
        param("value")
    }

    val noValue by node {}

    // TODO move to Kotlin?
    val unitValue by node {}

    val staticInit by abstractClass(ControlFlow.blockBody)
    val globalInit by node(staticInit)
    val threadLocalInit by node(staticInit)
    val standaloneThreadLocalInit by node(staticInit)

}