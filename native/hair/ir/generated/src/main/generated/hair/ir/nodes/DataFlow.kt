package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.Primitive

sealed class VarOp(form: Form, args: List<Node?>) : Spinal(form, args) {
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitVarOp(this)
}


class ReadVar internal constructor(form: Form) : VarOp(form, listOf()) {
    class Form internal constructor(metaForm: MetaForm, val variable: Var) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(variable)
    }
    
    val variable: Var by form::variable
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitReadVar(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ReadVar")
    }
}


class AssignVar internal constructor(form: Form, assignedValue: Node) : VarOp(form, listOf(assignedValue)) {
    class Form internal constructor(metaForm: MetaForm, val variable: Var) : MetaForm.ParametrisedControlFlowForm<Form>(metaForm) {
        override val args = listOf<Any>(variable)
    }
    
    val variable: Var by form::variable
    val assignedValueIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "assignedValue"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitAssignVar(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "AssignVar")
    }
}


class Phi internal constructor(form: Form, block: Block, vararg joinedValues: Node) : NodeBase(form, listOf(block, *joinedValues)) {
    val blockIndex: Int = 0
    val joinedValuesIndex: Int = 1
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "block"
        else -> "joinedValues"
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitPhi(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Phi")
    }
}


class Param internal constructor(form: Form) : NodeBase(form, listOf()) {
    class Form internal constructor(metaForm: MetaForm, val number: Int) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(number)
    }
    
    val number: Int by form::number
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitParam(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Param")
    }
}


