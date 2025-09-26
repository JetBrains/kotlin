package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.Primitive

class NoValue internal constructor(form: Form, ) : NodeBase(form, listOf()) {
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitNoValue(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "NoValue")
    }
}


class Placeholder internal constructor(form: Form, vararg inputs: Node) : NodeBase(form, listOf(*inputs)) {
    class Form internal constructor(metaForm: MetaForm, val tag: Any) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(tag)
    }
    
    val tag: Any by form::tag
    val inputsIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        else -> "inputs"
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitPlaceholder(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Placeholder")
    }
}


class Use internal constructor(form: Form, value: Node) : Spinal(form, listOf(value)) {
    val valueIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitUse(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Use")
    }
}


sealed class ProxyProjection(form: Form, args: List<Node?>) : NodeBase(form, args), Projection {
    override val ownerIndex: Int = 0
    val originIndex: Int = 1
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitProxyProjection(this)
}


