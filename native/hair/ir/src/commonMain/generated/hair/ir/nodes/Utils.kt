package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

class Use internal constructor(form: Form, control: Controlling?, value: Node?) : BlockBody(form, listOf(control, value)) {
    val valueIndex: Int = 1
    
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


sealed class StaticInit(form: Form, args: List<Node?>) : BlockBody(form, args) {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStaticInit(this)
}


class GlobalInit internal constructor(form: Form, control: Controlling?) : StaticInit(form, listOf(control)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitGlobalInit(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "GlobalInit")
    }
}


class ThreadLocalInit internal constructor(form: Form, control: Controlling?) : StaticInit(form, listOf(control)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitThreadLocalInit(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "ThreadLocalInit")
    }
}


class StandaloneThreadLocalInit internal constructor(form: Form, control: Controlling?) : StaticInit(form, listOf(control)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStandaloneThreadLocalInit(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "StandaloneThreadLocalInit")
    }
}


