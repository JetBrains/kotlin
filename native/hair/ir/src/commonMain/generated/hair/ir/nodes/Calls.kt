package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface AnyCall : Node {
    
    
    
}


sealed class AnyInvoke(form: Form, args: List<Node?>) : BlockBodyWithException(form, args), AnyCall {
    val callArgs: VarArgsList<Node>
        get() = VarArgsList(args, 1, Node::class)
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAnyInvoke(this)
}


class InvokeStatic internal constructor(form: Form, control: Controlling?, vararg callArgs: Node?) : AnyInvoke(form, listOf(control, *callArgs)) {
    class Form internal constructor(metaForm: MetaForm, val function: HairFunction) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(function)
    }
    
    val function: HairFunction by form::function
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> "callArgs"
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitInvokeStatic(this)
    
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "InvokeStatic")
    }
}


class InvokeVirtual internal constructor(form: Form, control: Controlling?, vararg callArgs: Node?) : AnyInvoke(form, listOf(control, *callArgs)) {
    class Form internal constructor(metaForm: MetaForm, val function: HairFunction) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(function)
    }
    
    val function: HairFunction by form::function
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> "callArgs"
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitInvokeVirtual(this)
    
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "InvokeVirtual")
    }
}


