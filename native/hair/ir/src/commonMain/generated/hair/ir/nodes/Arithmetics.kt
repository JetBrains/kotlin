package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

sealed interface ConstAny : Node {
    val value: Any
    
    
    
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


sealed interface AssociativeOp : Node {
    
    
    
}


sealed interface CommutativeOp : Node {
    
    
    
}


class ConstI internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Int) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    override val value: Int by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstI(this)
    
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstI")
    }
}


sealed class BinaryOpI(form: Form, args: List<Node?>) : BinaryOp(form, args) {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBinaryOpI(this)
}


class AddI internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpI(form, listOf(lhs, rhs)), CommutativeOp, AssociativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAddI(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "AddI")
    }
}


class SubI internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpI(form, listOf(lhs, rhs)), AssociativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSubI(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "SubI")
    }
}


class MulI internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpI(form, listOf(lhs, rhs)), CommutativeOp, AssociativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitMulI(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "MulI")
    }
}


class DivI internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpI(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDivI(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "DivI")
    }
}


class RemI internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpI(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitRemI(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "RemI")
    }
}


class ConstL internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Long) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    override val value: Long by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstL(this)
    
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstL")
    }
}


sealed class BinaryOpL(form: Form, args: List<Node?>) : BinaryOp(form, args) {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBinaryOpL(this)
}


class AddL internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpL(form, listOf(lhs, rhs)), CommutativeOp, AssociativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAddL(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "AddL")
    }
}


class SubL internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpL(form, listOf(lhs, rhs)), AssociativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSubL(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "SubL")
    }
}


class MulL internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpL(form, listOf(lhs, rhs)), CommutativeOp, AssociativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitMulL(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "MulL")
    }
}


class DivL internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpL(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDivL(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "DivL")
    }
}


class RemL internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpL(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitRemL(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "RemL")
    }
}


class ConstF internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Float) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    override val value: Float by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstF(this)
    
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstF")
    }
}


sealed class BinaryOpF(form: Form, args: List<Node?>) : BinaryOp(form, args) {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBinaryOpF(this)
}


class AddF internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpF(form, listOf(lhs, rhs)), CommutativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAddF(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "AddF")
    }
}


class SubF internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpF(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSubF(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "SubF")
    }
}


class MulF internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpF(form, listOf(lhs, rhs)), CommutativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitMulF(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "MulF")
    }
}


class DivF internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpF(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDivF(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "DivF")
    }
}


class RemF internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpF(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitRemF(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "RemF")
    }
}


class ConstD internal constructor(form: Form) : NodeBase(form, listOf()), ConstAny {
    class Form internal constructor(metaForm: MetaForm, val value: Double) : MetaForm.ParametrisedValueForm<Form>(metaForm) {
        override val args = listOf<Any>(value)
    }
    
    override val value: Double by form::value
    
    
    override fun paramName(index: Int): String = when (index) {
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitConstD(this)
    
    companion object {
        internal fun metaForm(session: Session) = MetaForm(session, "ConstD")
    }
}


sealed class BinaryOpD(form: Form, args: List<Node?>) : BinaryOp(form, args) {
    
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBinaryOpD(this)
}


class AddD internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpD(form, listOf(lhs, rhs)), CommutativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAddD(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "AddD")
    }
}


class SubD internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpD(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSubD(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "SubD")
    }
}


class MulD internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpD(form, listOf(lhs, rhs)), CommutativeOp {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitMulD(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "MulD")
    }
}


class DivD internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpD(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDivD(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "DivD")
    }
}


class RemD internal constructor(form: Form, lhs: Node?, rhs: Node?) : BinaryOpD(form, listOf(lhs, rhs)) {
    
    
    override fun paramName(index: Int): String = when (index) {
        0 -> "lhs"
        1 -> "rhs"
        else -> error("Unexpected arg index: $index")
    }
    
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitRemD(this)
    
    companion object {
        internal fun form(session: Session) = SimpleValueForm(session, "RemD")
    }
}


