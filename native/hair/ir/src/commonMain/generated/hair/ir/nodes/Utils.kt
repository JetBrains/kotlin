package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

class NoValue internal constructor(form: Form, ) : NodeBase(form, listOf()) {
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNoValue(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "NoValue")
    }
}


class UnitValue internal constructor(form: Form, ) : NodeBase(form, listOf()) {
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitUnitValue(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "UnitValue")
    }
}


class Use internal constructor(form: Form, control: Controlling?, value: Node?) : BlockBody(form, listOf(control, value)) {
    val value: Node
        get() = args[1]
    val valueOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
     var value: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
     var valueOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitUse(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Use")
    }
}


