package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

class StaticCall internal constructor(form: Form, control: Controlling?, vararg callArgs: Node?) : BlockBodyWithException(form, listOf(control, *callArgs)) {
    class Form internal constructor(metaForm: MetaForm, val function: HairFunction) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(function)
    }
    
    val function: HairFunction by form::function
    val callArgs: VarArgsList<Node>
        get() = VarArgsList(args, 1, Node::class)
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> "callArgs"
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStaticCall(this)
    
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "StaticCall")
    }
}


