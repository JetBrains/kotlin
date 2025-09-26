package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.Primitive

class StaticCall internal constructor(form: Form, lastLocationAccess: Node, vararg callArgs: Node) : ThrowingSpinal(form, listOf(lastLocationAccess, *callArgs)), MemoryAccess {
    class Form internal constructor(metaForm: MetaForm, val function: HairFunction) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(function)
    }
    
    val function: HairFunction by form::function
    override val lastLocationAccessIndex: Int = 0
    val callArgsIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        else -> "callArgs"
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitStaticCall(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "StaticCall")
    }
}


class Return internal constructor(form: Form, lastLocationAccess: Node, value: Node) : NoExit(form, listOf(lastLocationAccess, value)), MemoryAccess {
    override val lastLocationAccessIndex: Int = 0
    val valueIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        1 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitReturn(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Return")
    }
}


