package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface AnyNew : Node {
    
    
    
}


class New internal constructor(form: Form, control: Controlling?) : BlockBody(form, listOf(control)), AnyNew {
    class Form internal constructor(metaForm: MetaForm, val objectType: Class) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(objectType)
    }
    
    val objectType: Class by form::objectType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNew(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "New")
    }
}


sealed interface MemoryOp : Node {
    val field: MemoryLocation
    
    
    
}


sealed interface InstanceFieldOp : MemoryOp {
    override val field: Field
    val obj: Node
    val objOrNull: Node?
    context(_: ArgsUpdater)
     var obj: Node
    context(_: ArgsUpdater)
     var objOrNull: Node?
    
    
}


sealed interface GlobalFieldOp : MemoryOp {
    override val field: Global
    
    
    
}


sealed interface ReadMemory : MemoryOp {
    
    
    
}


sealed interface WriteMemory : MemoryOp {
    val value: Node
    val valueOrNull: Node?
    context(_: ArgsUpdater)
     var value: Node
    context(_: ArgsUpdater)
     var valueOrNull: Node?
    
    
}


sealed interface ReadField : ReadMemory, InstanceFieldOp {
    override val field: Field
    
    
    
}


sealed interface ReadGlobal : ReadMemory, GlobalFieldOp {
    override val field: Global
    
    
    
}


sealed class PinnedMemoryOp(form: Form, args: List<Node?>) : BlockBody(form, args), MemoryOp {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitPinnedMemoryOp(this)
}


sealed class PinnedInstanceFieldOp(form: Form, args: List<Node?>) : PinnedMemoryOp(form, args), InstanceFieldOp {
    override val obj: Node
        get() = args[1]
    override val objOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
    override var obj: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
    override var objOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitPinnedInstanceFieldOp(this)
}


sealed class PinnedGlobalFieldOp(form: Form, args: List<Node?>) : PinnedMemoryOp(form, args), GlobalFieldOp {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitPinnedGlobalFieldOp(this)
}


class ReadFieldPinned internal constructor(form: Form, control: Controlling?, obj: Node?) : PinnedInstanceFieldOp(form, listOf(control, obj)), ReadField {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitReadFieldPinned(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ReadFieldPinned")
    }
}


class ReadGlobalPinned internal constructor(form: Form, control: Controlling?) : PinnedGlobalFieldOp(form, listOf(control)), ReadGlobal {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitReadGlobalPinned(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ReadGlobalPinned")
    }
}


class WriteField internal constructor(form: Form, control: Controlling?, obj: Node?, value: Node?) : PinnedInstanceFieldOp(form, listOf(control, obj, value)), WriteMemory {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    override val value: Node
        get() = args[2]
    override val valueOrNull: Node?
        get() = args.getOrNull(2)
    context(_: ArgsUpdater)
    override var value: Node
        get() = args[2]
        set(value) { args[2] = value }
    context(_: ArgsUpdater)
    override var valueOrNull: Node?
        get() = args.getOrNull(2)
        set(value) { args[2] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "obj"
        2 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitWriteField(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "WriteField")
    }
}


class WriteGlobal internal constructor(form: Form, control: Controlling?, value: Node?) : PinnedGlobalFieldOp(form, listOf(control, value)), WriteMemory {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    override val value: Node
        get() = args[1]
    override val valueOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
    override var value: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
    override var valueOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitWriteGlobal(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "WriteGlobal")
    }
}


sealed class TypeCheck(form: Form, args: List<Node?>) : NodeBase(form, args) {
    val obj: Node
        get() = args[0]
    val objOrNull: Node?
        get() = args.getOrNull(0)
    context(_: ArgsUpdater)
     var obj: Node
        get() = args[0]
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var objOrNull: Node?
        get() = args.getOrNull(0)
        set(value) { args[0] = value }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTypeCheck(this)
}


class IsInstanceOf internal constructor(form: Form, obj: Node?) : TypeCheck(form, listOf(obj)) {
    class Form internal constructor(metaForm: MetaForm, val targetType: Reference) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    val targetType: Reference by form::targetType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitIsInstanceOf(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "IsInstanceOf")
    }
}


class CheckCast internal constructor(form: Form, obj: Node?) : TypeCheck(form, listOf(obj)) {
    class Form internal constructor(metaForm: MetaForm, val targetType: Reference) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    val targetType: Reference by form::targetType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCheckCast(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "CheckCast")
    }
}


sealed class ValueProxy(form: Form, args: List<Node?>) : BlockBody(form, args) {
    val origin: Node
        get() = args[1]
    val originOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
     var origin: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
     var originOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitValueProxy(this)
}


