package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface ControlFlow : Node {
    
    
}


sealed interface Projection : ControlFlow {
    val ownerIndex: Int
    
}


sealed interface Controlling : ControlFlow {
    
    
}


sealed interface Throwing : ControlFlow {
    
    
}


sealed interface BlockExit : ControlFlow {
    
    
}


class Unreachable internal constructor(form: Form, ) : NodeBase(form, listOf()), Controlling, BlockExit {
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitUnreachable(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Unreachable")
    }
}


class BlockEntry internal constructor(form: Form, vararg preds: BlockExit?) : NodeBase(form, listOf(*preds)), Controlling {
    val predsIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        else -> "preds"
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBlockEntry(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "BlockEntry")
    }
}


sealed class Controlled(form: Form, args: List<Node?>) : NodeBase(form, args), ControlFlow {
    val controlIndex: Int = 0
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitControlled(this)
}


sealed class BlockBody(form: Form, args: List<Node?>) : Controlled(form, args), Controlling {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBlockBody(this)
}


sealed class BlockBodyWithException(form: Form, args: List<Node?>) : BlockBody(form, args), Throwing {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBlockBodyWithException(this)
}


sealed class BlockEnd(form: Form, args: List<Node?>) : Controlled(form, args) {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBlockEnd(this)
}


class Return internal constructor(form: Form, control: Controlling?, result: Node?) : BlockEnd(form, listOf(control, result)) {
    val resultIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "result"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitReturn(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Return")
    }
}


class Goto internal constructor(form: Form, control: Controlling?) : BlockEnd(form, listOf(control)), BlockExit {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitGoto(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Goto")
    }
}


class If internal constructor(form: Form, control: Controlling?, cond: Node?) : BlockEnd(form, listOf(control, cond)) {
    val condIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "cond"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitIf(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "If")
    }
}


sealed class IfProjection(form: Form, args: List<Node?>) : NodeBase(form, args), Projection, BlockExit {
    override val ownerIndex: Int = 0
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitIfProjection(this)
}


class TrueExit internal constructor(form: Form, owner: If?) : IfProjection(form, listOf(owner)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "owner"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTrueExit(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "TrueExit")
    }
}


class FalseExit internal constructor(form: Form, owner: If?) : IfProjection(form, listOf(owner)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "owner"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitFalseExit(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "FalseExit")
    }
}


class Throw internal constructor(form: Form, control: Controlling?, exception: Node?) : BlockEnd(form, listOf(control, exception)), Throwing {
    val exceptionIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "exception"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitThrow(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Throw")
    }
}


class Unwind internal constructor(form: Form, thrower: Throwing?) : NodeBase(form, listOf(thrower)), BlockExit {
    val throwerIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "thrower"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitUnwind(this)
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Unwind")
    }
}


