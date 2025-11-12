package hair.ir

import hair.ir.nodes.*
import hair.utils.ensuring

interface IrTest {
    fun withTestSession(test: Session.() -> Unit) {
        Session().test()
    }
}