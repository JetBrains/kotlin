package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL
import hair.sym.HairStaticInitializer

object Utils : ModelDSL() {

    // used in tests
    val use by node(ControlFlow.blockBody) {
        param("value")
    }

    val noValue by node {
        interfaces(DataFlow.valueNode)
    }

    // TODO move to Kotlin?
    val unitValue by node {
        interfaces(DataFlow.valueNode)
    }

    val staticInit by abstractClass(ControlFlow.blockBodyWithException) {
        formParam("initRoutine", HairStaticInitializer::class)
    }
    val globalInit by node(staticInit)
    val threadLocalInit by node(staticInit)
    val standaloneThreadLocalInit by node(staticInit)

}
