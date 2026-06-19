package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface ConstAny : ValueNode {
    
    
}


class Const internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Number) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    val value: Number by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConst(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Const")
    }
}


class Null internal constructor(form: Form, ) : NodeBase(form, listOf()), ConstAny {
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNull(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Null")
    }
}


sealed class ConstBoolean(form: Form, args: List<Node?>) : NodeBase(form, args), ConstAny {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstBoolean(this)
}


class True internal constructor(form: Form, ) : ConstBoolean(form, listOf()) {
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTrue(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "True")
    }
}


class False internal constructor(form: Form, ) : ConstBoolean(form, listOf()) {
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitFalse(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "False")
    }
}


sealed class BinaryOp(form: Form, args: List<Node?>) : NodeBase(form, args), ValueNode {
    val lhsIndex: Int = 0
    val rhsIndex: Int = 1
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBinaryOp(this)
}


sealed class ArithBinaryOp(form: Form, args: List<Node?>) : BinaryOp(form, args) {
    abstract val opType: ArithmeticType
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitArithBinaryOp(this)
}


class Add internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAdd(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Add")
    }
}


class Sub internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSub(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Sub")
    }
}


class Mul internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitMul(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Mul")
    }
}


class Div internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDiv(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Div")
    }
}


class Rem internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitRem(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Rem")
    }
}


class Neg internal constructor(form: Form, operand: Node?) : NodeBase(form, listOf(operand)) {
    val operandIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNeg(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Neg")
    }
}


class And internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAnd(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "And")
    }
}


class Or internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitOr(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Or")
    }
}


class Xor internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitXor(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Xor")
    }
}


class Shl internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitShl(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Shl")
    }
}


class Shr internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitShr(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Shr")
    }
}


class Ushr internal constructor(form: Form, lhs: Node?, rhs: Node?) : ArithBinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val opType: ArithmeticType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(opType)
    }
    
    override val opType: ArithmeticType by form::opType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitUshr(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Ushr")
    }
}


class Inv internal constructor(form: Form, operand: Node?) : NodeBase(form, listOf(operand)), ValueNode {
    val operandIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitInv(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Inv")
    }
}


class Cmp internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType, val op: CmpOp) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type, op)
    }
    
    val type: HairType by form::type
    val op: CmpOp by form::op
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCmp(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Cmp")
    }
}


class Not internal constructor(form: Form, operand: Node?) : NodeBase(form, listOf(operand)), ValueNode {
    val operandIndex: Int = 0
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNot(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Not")
    }
}


sealed class Cast(form: Form, args: List<Node?>) : NodeBase(form, args), ValueNode {
    abstract val targetType: HairType
    val operandIndex: Int = 0
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCast(this)
}


class SignExtend internal constructor(form: Form, operand: Node?) : Cast(form, listOf(operand)) {
    class Form internal constructor(metaForm: MetaForm, val targetType: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    override val targetType: HairType by form::targetType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSignExtend(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "SignExtend")
    }
}


class ZeroExtend internal constructor(form: Form, operand: Node?) : Cast(form, listOf(operand)) {
    class Form internal constructor(metaForm: MetaForm, val targetType: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    override val targetType: HairType by form::targetType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitZeroExtend(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ZeroExtend")
    }
}


class Truncate internal constructor(form: Form, operand: Node?) : Cast(form, listOf(operand)) {
    class Form internal constructor(metaForm: MetaForm, val targetType: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    override val targetType: HairType by form::targetType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTruncate(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Truncate")
    }
}


class Reinterpret internal constructor(form: Form, operand: Node?) : Cast(form, listOf(operand)) {
    class Form internal constructor(metaForm: MetaForm, val targetType: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    override val targetType: HairType by form::targetType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitReinterpret(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Reinterpret")
    }
}


