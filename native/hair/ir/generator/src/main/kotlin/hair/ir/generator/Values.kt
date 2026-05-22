package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL

object Values : ModelDSL() {

    // Marker for value-producing nodes. The `valueType: HairType` extension property
    // is defined centrally in `hair.ir.nodes.ValueType` (hand-written) and dispatches
    // on the concrete node class.
    val valueNode by nodeInterface()
}
