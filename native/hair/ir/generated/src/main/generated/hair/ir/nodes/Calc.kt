package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.Primitive

class ConstInt internal constructor(form: Form) : NodeBase(form, listOf()) {
    class Form internal constructor(metaForm: MetaForm, val value: Long) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    val value: Long by form::value
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitConstInt(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstInt")
    }
}


class ConstFloat internal constructor(form: Form) : NodeBase(form, listOf()) {
    class Form internal constructor(metaForm: MetaForm, val value: Double) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    val value: Double by form::value
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitConstFloat(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstFloat")
    }
}


sealed class ArithmeticOp(form: Form, args: List<Node?>) : NodeBase(form, args) {
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitArithmeticOp(this)
}


sealed class BinaryOp(form: Form, args: List<Node?>) : ArithmeticOp(form, args) {
    val lhsIndex: Int = 0
    val rhsIndex: Int = 1
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitBinaryOp(this)
}


sealed interface AssociativeOp : Node {
    
}


sealed interface CommutativeOp : Node {
    
}


class Add internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)), AssociativeOp, CommutativeOp {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitAdd(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Add")
    }
}


class Sub internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)), AssociativeOp {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitSub(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Sub")
    }
}


class Mul internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)), AssociativeOp, CommutativeOp {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitMul(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Mul")
    }
}


class Div internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitDiv(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Div")
    }
}


class Rem internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitRem(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Rem")
    }
}


class And internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)), AssociativeOp, CommutativeOp {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitAnd(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "And")
    }
}


class Or internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)), AssociativeOp, CommutativeOp {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitOr(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Or")
    }
}


class Xor internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)), AssociativeOp, CommutativeOp {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitXor(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Xor")
    }
}


class Shl internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitShl(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Shl")
    }
}


class Shr internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitShr(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Shr")
    }
}


class Ushr internal constructor(form: Form, lhs: Node, rhs: Node) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: Primitive) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: Primitive by form::type
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visitUshr(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Ushr")
    }
}


