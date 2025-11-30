package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface ConstAny : Node {
    
    
    
}


class ConstI internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Int) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    val value: Int by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstI(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstI")
    }
}


class ConstL internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Long) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    val value: Long by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstL(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstL")
    }
}


class ConstF internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Float) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    val value: Float by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstF(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstF")
    }
}


class ConstD internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Double) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    val value: Double by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstD(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstD")
    }
}


class True internal constructor(form: Form, ) : NodeBase(form, listOf()), ConstAny {
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTrue(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "True")
    }
}


class False internal constructor(form: Form, ) : NodeBase(form, listOf()), ConstAny {
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitFalse(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "False")
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


sealed class BinaryOp(form: Form, args: List<Node?>) : NodeBase(form, args) {
    val lhs: Node
        get() = args[0]
    val lhsOrNull: Node?
        get() = args.getOrNull(0)
    context(_: ArgsUpdater)
     var lhs: Node
        get() = args[0]
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var lhsOrNull: Node?
        get() = args.getOrNull(0)
        set(value) { args[0] = value }
    val rhs: Node
        get() = args[1]
    val rhsOrNull: Node?
        get() = args.getOrNull(1)
    context(_: ArgsUpdater)
     var rhs: Node
        get() = args[1]
        set(value) { args[1] = value }
    context(_: ArgsUpdater)
     var rhsOrNull: Node?
        get() = args.getOrNull(1)
        set(value) { args[1] = value }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBinaryOp(this)
}


class Add internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Sub internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Mul internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Div internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Rem internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class And internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Or internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Xor internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Shl internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Shr internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Ushr internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOp(form, listOf(lhs, rhs)) {
    class Form internal constructor(metaForm: MetaForm, val type: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(type)
    }
    
    val type: HairType by form::type
    
    
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


class Not internal constructor(form: Form, operand: Node?) : NodeBase(form, listOf(operand)) {
    val operand: Node
        get() = args[0]
    val operandOrNull: Node?
        get() = args.getOrNull(0)
    context(_: ArgsUpdater)
     var operand: Node
        get() = args[0]
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var operandOrNull: Node?
        get() = args.getOrNull(0)
        set(value) { args[0] = value }
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNot(this)
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "Not")
    }
}


sealed class Cast(form: Form, args: List<Node?>) : NodeBase(form, args) {
    val operand: Node
        get() = args[0]
    val operandOrNull: Node?
        get() = args.getOrNull(0)
    context(_: ArgsUpdater)
     var operand: Node
        get() = args[0]
        set(value) { args[0] = value }
    context(_: ArgsUpdater)
     var operandOrNull: Node?
        get() = args.getOrNull(0)
        set(value) { args[0] = value }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCast(this)
}


class SignExtend internal constructor(form: Form, operand: Node?) : Cast(form, listOf(operand)) {
    class Form internal constructor(metaForm: MetaForm, val targetType: HairType) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(targetType)
    }
    
    val targetType: HairType by form::targetType
    
    
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
    
    val targetType: HairType by form::targetType
    
    
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
    
    val targetType: HairType by form::targetType
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "operand"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTruncate(this)
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "Truncate")
    }
}


