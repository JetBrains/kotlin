package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface MemoryOp : Node {
    
    
}


sealed interface AnyLoad : MemoryOp, ValueNode {
    
    
}


sealed interface AnyStore : MemoryOp {
    val valueIndex: Int
    
}


sealed class PinnedMemoryOp(form: Form, args: List<Node?>) : BlockBody(form, args), MemoryOp {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitPinnedMemoryOp(this)
}


sealed class DirectMemoryOp(form: Form, args: List<Node?>) : PinnedMemoryOp(form, args), MemoryOp {
    abstract val type: HairType
    val locationIndex: Int = 1
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDirectMemoryOp(this)
}


class Load internal constructor(form: Form, control: Controlling?, location: Node?) : DirectMemoryOp(form, listOf(control, location)), AnyLoad {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    override val type: HairType by form::type
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "location"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLoad(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Load")
    }
}


class Store internal constructor(form: Form, control: Controlling?, location: Node?, value: Node?) : DirectMemoryOp(form, listOf(control, location, value)), AnyStore {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    override val type: HairType by form::type
    override val valueIndex: Int = 2
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "location"
        2 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStore(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Store")
    }
}


sealed interface InstanceFieldOp : MemoryOp {
    val field: Field
    val objIndex: Int
    
}


class LoadField internal constructor(form: Form, control: Controlling?, obj: Node?) : PinnedMemoryOp(form, listOf(control, obj)), InstanceFieldOp, AnyLoad {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    override val objIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLoadField(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "LoadField")
    }
}


class StoreField internal constructor(form: Form, control: Controlling?, obj: Node?, value: Node?) : PinnedMemoryOp(form, listOf(control, obj, value)), InstanceFieldOp, AnyStore {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    override val objIndex: Int = 1
    override val valueIndex: Int = 2
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "obj"
        2 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStoreField(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "StoreField")
    }
}


sealed interface GlobalOp : MemoryOp {
    val field: Global
    
    
}


class LoadGlobal internal constructor(form: Form, control: Controlling?) : PinnedMemoryOp(form, listOf(control)), GlobalOp, AnyLoad {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLoadGlobal(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "LoadGlobal")
    }
}


class StoreGlobal internal constructor(form: Form, control: Controlling?, value: Node?) : PinnedMemoryOp(form, listOf(control, value)), GlobalOp, AnyStore {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    override val valueIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStoreGlobal(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "StoreGlobal")
    }
}


sealed class ArrayMemoryOp(form: Form, args: List<Node?>) : PinnedMemoryOp(form, args) {
    abstract val elementType: HairType
    val arrayIndex: Int = 1
    val indexIndex: Int = 2
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitArrayMemoryOp(this)
}


class LoadArrayElement internal constructor(form: Form, control: Controlling?, array: Node?, index: Node?) : ArrayMemoryOp(form, listOf(control, array, index)), AnyLoad {
    class Form internal constructor(metaForm: MetaForm, val elementType: HairType) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(elementType)
    }
    
    override val elementType: HairType by form::elementType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "array"
        2 -> "index"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLoadArrayElement(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "LoadArrayElement")
    }
}


class StoreArrayElement internal constructor(form: Form, control: Controlling?, array: Node?, index: Node?, value: Node?) : ArrayMemoryOp(form, listOf(control, array, index, value)), AnyStore {
    class Form internal constructor(metaForm: MetaForm, val elementType: HairType) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(elementType)
    }
    
    override val elementType: HairType by form::elementType
    override val valueIndex: Int = 3
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "control"
        1 -> "array"
        2 -> "index"
        3 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStoreArrayElement(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "StoreArrayElement")
    }
}


