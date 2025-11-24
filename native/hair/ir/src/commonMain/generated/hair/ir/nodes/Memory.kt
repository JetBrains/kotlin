package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface MemoryOp : Node {
    
    
    
}


sealed interface AnyLoad : MemoryOp {
    
    
    
}


sealed interface AnyStore : MemoryOp {
    val value: Node
    val valueOrNull: Node?
    
    
}


sealed class DirectMemoryOp(form: Form, args: List<Node?>) : NodeBase(form, args), MemoryOp {
    val location: Node
        get() = args[0]
    val locationOrNull: Node?
        get() = args.getOrNull(0)
    context(_: ArgsUpdater)
     var location: Node
        get() = args[0]
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var locationOrNull: Node?
        get() = args.getOrNull(0)
        set(value) { args[0] = value }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDirectMemoryOp(this)
}


class Load internal constructor(form: Form, location: Node?) : DirectMemoryOp(form, listOf(location)), AnyLoad {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "location"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLoad(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Load")
    }
}


class Store internal constructor(form: Form, location: Node?) : DirectMemoryOp(form, listOf(location)), AnyLoad {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "location"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStore(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Store")
    }
}


sealed interface InstanceFieldOp : MemoryOp {
    val field: Field
    val obj: Node
    val objOrNull: Node?
    
    
}


class LoadField internal constructor(form: Form, obj: Node?) : NodeBase(form, listOf(obj)), InstanceFieldOp, AnyLoad {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    override val obj: Node
        get() = args[0]
    override val objOrNull: Node?
        get() = args.getOrNull(0)
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "obj"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLoadField(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "LoadField")
    }
}


class StoreField internal constructor(form: Form, obj: Node?, value: Node?) : NodeBase(form, listOf(obj, value)), InstanceFieldOp, AnyStore {
    class Form internal constructor(metaForm: MetaForm, val field: Field) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Field by form::field
    override val obj: Node
        get() = args[0]
    override val objOrNull: Node?
        get() = args.getOrNull(0)
    override val value: Node
        get() = args[1]
    override val valueOrNull: Node?
        get() = args.getOrNull(1)
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "obj"
        1 -> "value"
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


class LoadGlobal internal constructor(form: Form) : NodeBase(form, listOf()), GlobalOp, AnyLoad {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLoadGlobal(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "LoadGlobal")
    }
}


class StoreGlobal internal constructor(form: Form, value: Node?) : NodeBase(form, listOf(value)), GlobalOp, AnyStore {
    class Form internal constructor(metaForm: MetaForm, val field: Global) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(field)
    }
    
    override val field: Global by form::field
    override val value: Node
        get() = args[0]
    override val valueOrNull: Node?
        get() = args.getOrNull(0)
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "value"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStoreGlobal(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "StoreGlobal")
    }
}


