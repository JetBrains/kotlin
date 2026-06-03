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
    val assignedValueIndex: Int = 1
    
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
    val blockIndex: Int = 0
    val joinedValuesIndex: Int = 1
    
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
    val blockIndex: Int = 0
    val joinedValuesIndex: Int = 1
    
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
    val unwindIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "unwind"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCatch(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Catch")
    }
}


