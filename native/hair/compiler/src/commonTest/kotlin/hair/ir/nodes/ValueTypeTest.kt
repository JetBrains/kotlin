package hair.ir.nodes

import hair.ir.IrTest
import hair.sym.Field
import hair.sym.Global
import hair.sym.HairClass
import hair.sym.HairType

// TODO move to a common place
private data class TestField(override val owner: HairClass, override val type: HairType) : Field
private data class TestGlobal(override val type: HairType) : Global

class ValueTypeTest : IrTest {

    // TODO write proper tests
}
