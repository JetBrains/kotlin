package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface AnyNew : Node {
    
    
    
}


class New internal constructor(form: Form, control: Controlling?) : BlockBody(form, listOf(control)), AnyNew {
    class Form internal constructor(metaForm: MetaForm, val objectType: HairClass) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(objectType)
    }
    
    val objectType: HairClass by form::objectType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNew(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "New")
    }
}


class NewArray internal constructor(form: Form, control: Controlling?, size: Node?) : BlockBody(form, listOf(control, size)), AnyNew {
    class Form internal constructor(metaForm: MetaForm, val elementType: HairClass) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(elementType)
    }
    
    val elementType: HairClass by form::elementType
    val size: Node
        get() = args[1]
    val sizeOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
     var size: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
     var sizeOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "size"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNewArray(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "NewArray")
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
    class Form internal constructor(metaForm: MetaForm, val targetType: HairClass) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    val targetType: HairClass by form::targetType
    
    
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
    class Form internal constructor(metaForm: MetaForm, val targetType: HairClass) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    val targetType: HairClass by form::targetType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCheckCast(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "CheckCast")
    }
}


