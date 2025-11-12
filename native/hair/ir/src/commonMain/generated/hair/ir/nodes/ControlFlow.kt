package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface ControlFlow : Node {
    
    
    
}


sealed interface Projection : ControlFlow {
    val owner: ControlFlow
    val ownerOrNull: ControlFlow?
    
    
}


sealed interface Controlling : ControlFlow {
    
    
    
}


sealed interface Throwing : ControlFlow {
    
    
    
}


sealed interface BlockExit : ControlFlow {
    
    
    
}


class BlockEntry internal constructor(form: Form, vararg preds: BlockExit?) : NodeBase(form, listOf(*preds)), Controlling {
    val preds: VarArgsList<BlockExit>
        get() = VarArgsList(args, 0, BlockExit::class)
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> "preds"
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBlockEntry(this)
    
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "BlockEntry")
    }
}


sealed class Controlled(form: Form, args: List<Node?>) : NodeBase(form, args), ControlFlow {
    val control: Controlling
        get() = args[0] as Controlling
    val controlOrNull: Controlling?
        get() = args.getOrNull(0)?.let { it as Controlling }
    context(_: ArgsUpdater)
     var control: Controlling
        get() = args[0] as Controlling
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var controlOrNull: Controlling?
        get() = args.getOrNull(0)?.let { it as Controlling }
        set(value) { args[0] = value }
    
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
    val result: Node
        get() = args[1]
    val resultOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
     var result: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
     var resultOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
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


sealed class IfProjection(form: Form, args: List<Node?>) : NodeBase(form, args), Projection, BlockExit {
    override val owner: If
        get() = args[0] as If
    override val ownerOrNull: If?
        get() = args.getOrNull(0)?.let { it as If }
    context(_: ArgsUpdater)
     var owner: If
        get() = args[0] as If
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var ownerOrNull: If?
        get() = args.getOrNull(0)?.let { it as If }
        set(value) { args[0] = value }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitIfProjection(this)
}


class If internal constructor(form: Form, control: Controlling?, cond: Node?) : BlockEnd(form, listOf(control, cond)) {
    val cond: Node
        get() = args[1]
    val condOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
     var cond: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
     var condOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "cond"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitIf(this)
    class True internal constructor(form: Form, owner: If?) : IfProjection(form, listOf(owner)) {
        
        
        override fun paramName(index: Int): String = when (index) {
            0 -> "owner"
            else -> error("Unexpected arg index: $index")
        }
        
        override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitIfTrue(this)
        
        companion object {
            internal fun form(session: Session) = SimpleControlFlowForm(session, "If.True")
        }
    }
    val trueExit = True(session.ifTrueForm, this).register()
    class False internal constructor(form: Form, owner: If?) : IfProjection(form, listOf(owner)) {
        
        
        override fun paramName(index: Int): String = when (index) {
            0 -> "owner"
            else -> error("Unexpected arg index: $index")
        }
        
        override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitIfFalse(this)
        
        companion object {
            internal fun form(session: Session) = SimpleControlFlowForm(session, "If.False")
        }
    }
    val falseExit = False(session.ifFalseForm, this).register()
    
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "If")
    }
}


class Throw internal constructor(form: Form, control: Controlling?, exception: Node?) : BlockEnd(form, listOf(control, exception)), Throwing {
    val exception: Node
        get() = args[1]
    val exceptionOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
     var exception: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
     var exceptionOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
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
    val thrower: Throwing
        get() = args[0] as Throwing
    val throwerOrNull: Throwing?
        get() = args.getOrNull(0)?.let { it as Throwing }
    context(_: ArgsUpdater)
     var thrower: Throwing
        get() = args[0] as Throwing
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var throwerOrNull: Throwing?
        get() = args.getOrNull(0)?.let { it as Throwing }
        set(value) { args[0] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "thrower"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitUnwind(this)
    
    companion object {
        internal fun form(session: Session) = SimpleControlFlowForm(session, "Unwind")
    }
}


