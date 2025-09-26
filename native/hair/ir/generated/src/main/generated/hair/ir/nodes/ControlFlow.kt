package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.Primitive

class Goto internal constructor(form: Form, ) : SingleExit(form, listOf()) {
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitGoto(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Goto")
    }
}


class If internal constructor(form: Form, condition: Node) : TwoExits(form, listOf(condition)) {
    val conditionIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "condition"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitIf(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "If")
    }
}


class Halt internal constructor(form: Form, ) : NoExit(form, listOf()) {
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitHalt(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Halt")
    }
}


sealed class Block(form: Form, args: List<Node?>) : ControlMerge(form, args) {
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitBlock(this)
}


class BBlock internal constructor(form: Form, ) : Block(form, listOf()) {
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitBBlock(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "BBlock")
    }
}


class XBlock internal constructor(form: Form, ) : Block(form, listOf()) {
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitXBlock(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "XBlock")
    }
}


class Throw internal constructor(form: Form, exception: Node) : ThrowExit(form, listOf(exception)) {
    val exceptionIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "exception"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitThrow(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Throw")
    }
}


class Catch internal constructor(form: Form, xBlock: XBlock, vararg catchedValues: Node) : NodeBase(form, listOf(xBlock, *catchedValues)) {
    val xBlockIndex: Int = 0
    val catchedValuesIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "xBlock"
        else -> "catchedValues"
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitCatch(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Catch")
    }
}


