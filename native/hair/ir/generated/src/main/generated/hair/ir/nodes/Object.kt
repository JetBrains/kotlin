package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.Primitive

sealed interface MemoryAccess : Node {
    val lastLocationAccessIndex: Int
    
}


sealed interface AnyNew : ControlFlow, MemoryAccess {
    
}


class New internal constructor(form: Form, lastLocationAccess: Node) : Spinal(form, listOf(lastLocationAccess)), AnyNew {
    class Form internal constructor(metaForm: MetaForm, val type: Class) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Class by form::type
    override val lastLocationAccessIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitNew(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "New")
    }
}


sealed interface MemoryOp : MemoryAccess {
    val field: MemoryLocation
    
}


sealed interface InstanceFieldOp : MemoryOp {
    override val field: Field
    val objIndex: Int
    
}


sealed interface GlobalFieldOp : MemoryOp {
    override val field: Global
    
}


sealed interface ReadMemory : MemoryOp {
    
}


sealed interface WriteMemory : MemoryOp, ControlFlow {
    val valueIndex: Int
    
}


sealed interface ReadField : ReadMemory, InstanceFieldOp {
    override val field: Field
    
}


sealed interface ReadGlobal : ReadMemory, GlobalFieldOp {
    override val field: Global
    
}


sealed class PinnedMemoryOp(form: Form, args: List<Node?>) : Spinal(form, args), MemoryOp {
    override val lastLocationAccessIndex: Int = 0
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitPinnedMemoryOp(this)
}


sealed class PinnedInstanceFieldOp(form: Form, args: List<Node?>) : PinnedMemoryOp(form, args), InstanceFieldOp {
    override val objIndex: Int = 1
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitPinnedInstanceFieldOp(this)
}


sealed class PinnedGlobalFieldOp(form: Form, args: List<Node?>) : PinnedMemoryOp(form, args), GlobalFieldOp {
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitPinnedGlobalFieldOp(this)
}


class ReadFieldPinned internal constructor(form: Form, lastLocationAccess: Node, obj: Node) : PinnedInstanceFieldOp(form, listOf(lastLocationAccess, obj)), ReadField {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        1 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitReadFieldPinned(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ReadFieldPinned")
    }
}


class ReadGlobalPinned internal constructor(form: Form, lastLocationAccess: Node) : PinnedGlobalFieldOp(form, listOf(lastLocationAccess)), ReadGlobal {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitReadGlobalPinned(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ReadGlobalPinned")
    }
}


class WriteField internal constructor(form: Form, lastLocationAccess: Node, obj: Node, value: Node) : PinnedInstanceFieldOp(form, listOf(lastLocationAccess, obj, value)), WriteMemory {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    override val valueIndex: Int = 2
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        1 -> "obj"
        2 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitWriteField(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "WriteField")
    }
}


class WriteGlobal internal constructor(form: Form, lastLocationAccess: Node, value: Node) : PinnedGlobalFieldOp(form, listOf(lastLocationAccess, value)), WriteMemory {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    override val valueIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        1 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitWriteGlobal(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "WriteGlobal")
    }
}


sealed class FloatingMemoryRead(form: Form, args: List<Node?>) : NodeBase(form, args), ReadMemory {
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitFloatingMemoryRead(this)
}


class ReadFieldFloating internal constructor(form: Form, lastLocationAccess: Node, obj: Node) : FloatingMemoryRead(form, listOf(lastLocationAccess, obj)), ReadField {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    override val lastLocationAccessIndex: Int = 0
    override val objIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        1 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitReadFieldFloating(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ReadFieldFloating")
    }
}


class ReadGlobalFloating internal constructor(form: Form, lastLocationAccess: Node) : FloatingMemoryRead(form, listOf(lastLocationAccess)), ReadGlobal {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    override val lastLocationAccessIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lastLocationAccess"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitReadGlobalFloating(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ReadGlobalFloating")
    }
}


class IsInstance internal constructor(form: Form, obj: Node) : Spinal(form, listOf(obj)) {
    class Form internal constructor(metaForm: MetaForm, val type: Class) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Class by form::type
    val objIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitIsInstance(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "IsInstance")
    }
}


class Cast internal constructor(form: Form, obj: Node) : Spinal(form, listOf(obj)) {
    class Form internal constructor(metaForm: MetaForm, val type: Class) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Class by form::type
    val objIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitCast(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Cast")
    }
}


class IndistinctMemory internal constructor(form: Form, vararg inputs: Node) : NodeBase(form, listOf(*inputs)) {
    val inputsIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        else -> "inputs"
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitIndistinctMemory(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "IndistinctMemory")
    }
}


class Unknown internal constructor(form: Form, ) : NodeBase(form, listOf()) {
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitUnknown(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Unknown")
    }
}


class Escape internal constructor(form: Form, owner: ControlFlow, origin: Node, into: Node) : ProxyProjection(form, listOf(owner, origin, into)) {
    val intoIndex: Int = 2
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "owner"
        1 -> "origin"
        2 -> "into"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitEscape(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Escape")
    }
}


class Overwrite internal constructor(form: Form, owner: ControlFlow, origin: Node) : ProxyProjection(form, listOf(owner, origin)) {
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "owner"
        1 -> "origin"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitOverwrite(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Overwrite")
    }
}


class NeqFilter internal constructor(form: Form, owner: ControlFlow, origin: Node, to: Node) : ProxyProjection(form, listOf(owner, origin, to)) {
    val toIndex: Int = 2
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "owner"
        1 -> "origin"
        2 -> "to"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitNeqFilter(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "NeqFilter")
    }
}


