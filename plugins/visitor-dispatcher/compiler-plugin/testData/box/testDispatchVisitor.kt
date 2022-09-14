// WITH_STDLIB
// DUMP_IR

import org.jetbrains.kotlin.dispatcher.*

// Fir elements
enum class ExprId {
    BinOp, Number, String, UnOp
}

@WithAbstractKind<ExprId>
interface FirElement

@WithAbstractKind<ExprId>
abstract class FirLiteral: FirElement

@WithKind<ExprId>("Number")
class FirNumber: FirLiteral()
@WithKind<ExprId>("String")
class FirString: FirLiteral()

@WithAbstractKind<ExprId>
abstract class FirOp: FirElement

@WithKind<ExprId>("BinOp")
class FirBinOp(val l: FirElement, val r: FirElement): FirOp()
@WithKind<ExprId>("UnOp")
class FirUnOp(val e: FirElement): FirOp()

// Fir abstract visitor
@DispatchedVisitor<FirElement>
abstract class FirVisitorVoid {
    abstract fun visitElement(element: FirElement)

    // Intermediate nodes
    open fun visitLiteral(literal: FirLiteral) = visitElement(literal)

    open fun visitOp(op: FirOp) = visitElement(op)

    // Leaf nodes
    @Dispatched
    open fun visitBinOp(binOp: FirBinOp) = visitElement(binOp)
    @Dispatched
    open fun visitUnOp(unOp: FirUnOp) = visitElement(unOp)

    @Dispatched
    open fun visitNumber(number: FirNumber) = visitElement(number)
    @Dispatched
    open fun visitString(string: FirString) = visitElement(string)
}

abstract class FirDefaultVisitorVoid: FirVisitorVoid() {
    override fun visitBinOp(binOp: FirBinOp) = visitOp(binOp)
    override fun visitUnOp(unOp: FirUnOp) = visitOp(unOp)
}

var result = ""

@GenerateDispatchFunction
class MyCustomVisitor: FirDefaultVisitorVoid() {
    override fun visitElement(element: FirElement) {}

    override fun visitOp(op: FirOp) {
        when (op) {
            is FirBinOp -> {
                result += "BinOp"
                // later it should be done with acceptChildren
                dispatch(op.l)
                dispatch(op.r)
            }
            is FirUnOp -> {
                result += "UnOp"
                // later it should be done with acceptChildren
                dispatch(op.e)
            }
        }


    }
    override fun visitString(string: FirString) {
        result += "String"
    }
    override fun visitNumber(number: FirNumber) {
        result += "Number"
    }
}

fun box(): String {
    val visitor = MyCustomVisitor()
    val expr = FirBinOp(FirUnOp(FirNumber()), FirString())
    visitor.dispatch(expr)

    return when (result) {
        "BinOpUnOpNumberString" -> "OK"
        else -> "FAIL"
    }
}