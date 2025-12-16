package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed class VarOp(form: Form, args: List<Node?>) : BlockBody(form, args) {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitVarOp(this)
}


class ReadVar internal constructor(form: Form, control: Controlling?) : VarOp(form, listOf(control)) {
    class Form internal constructor(metaForm: MetaForm, val variable: Any) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(variable)
    }
    
    val variable: Any by form::variable
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitReadVar(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ReadVar")
    }
}


class AssignVar internal constructor(form: Form, control: Controlling?, assignedValue: Node?) : VarOp(form, listOf(control, assignedValue)) {
    class Form internal constructor(metaForm: MetaForm, val variable: Any) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(variable)
    }
    
    val variable: Any by form::variable
    val assignedValue: Node
        get() = args[1]
    val assignedValueOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
     var assignedValue: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
     var assignedValueOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "assignedValue"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAssignVar(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "AssignVar")
    }
}


class Phi internal constructor(form: Form, block: BlockEntry?, vararg joinedValues: Node?) : NodeBase(form, listOf(block, *joinedValues)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    val block: BlockEntry
        get() = args[0] as BlockEntry
    val blockOrNull: BlockEntry?
        get() = args.getOrNull(0)?.let { it as BlockEntry }
    context(_: ArgsUpdater)
     var block: BlockEntry
        get() = args[0] as BlockEntry
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var blockOrNull: BlockEntry?
        get() = args.getOrNull(0)?.let { it as BlockEntry }
        set(value) { args[0] = value }
    val joinedValues: VarArgsList<Node>
        get() = VarArgsList(args, 1, Node::class)
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "block"
        else -> "joinedValues"
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitPhi(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Phi")
    }
}


class PhiPlaceholder internal constructor(form: Form, block: BlockEntry?, vararg joinedValues: Node?) : NodeBase(form, listOf(block, *joinedValues)) {
    class Form internal constructor(metaForm: MetaForm, val origin: Any) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(origin)
    }
    
    val origin: Any by form::origin
    val block: BlockEntry
        get() = args[0] as BlockEntry
    val blockOrNull: BlockEntry?
        get() = args.getOrNull(0)?.let { it as BlockEntry }
    context(_: ArgsUpdater)
     var block: BlockEntry
        get() = args[0] as BlockEntry
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var blockOrNull: BlockEntry?
        get() = args.getOrNull(0)?.let { it as BlockEntry }
        set(value) { args[0] = value }
    val joinedValues: VarArgsList<Node>
        get() = VarArgsList(args, 1, Node::class)
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "block"
        else -> "joinedValues"
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitPhiPlaceholder(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "PhiPlaceholder")
    }
}


class Param internal constructor(form: Form) : NodeBase(form, listOf()) {
    class Form internal constructor(metaForm: MetaForm, val index: Int) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(index)
    }
    
    val index: Int by form::index
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitParam(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Param")
    }
}


class Catch internal constructor(form: Form, unwind: Node?) : NodeBase(form, listOf(unwind)) {
    val unwind: Node
        get() = args[0]
    val unwindOrNull: Node?
        get() = args.getOrNull(0)
    context(_: ArgsUpdater)
     var unwind: Node
        get() = args[0]
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var unwindOrNull: Node?
        get() = args.getOrNull(0)
        set(value) { args[0] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "unwind"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCatch(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Catch")
    }
}


