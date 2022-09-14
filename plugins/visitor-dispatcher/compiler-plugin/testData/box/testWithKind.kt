// WITH_STDLIB
// DUMP_IR

import org.jetbrains.kotlin.dispatcher.DispatchedVisitor
import org.jetbrains.kotlin.dispatcher.Dispatched
import org.jetbrains.kotlin.dispatcher.WithKind
import org.jetbrains.kotlin.dispatcher.WithAbstractKind

// Fir elements
enum class ExprId {
    BinOp, Number, String
}

@WithAbstractKind<ExprId>
interface FirElement

@WithKind<ExprId>("BinOp")
class FirBinOp: FirElement

@WithKind<ExprId>("Number")
class FirNumber: FirElement

@WithKind<ExprId>("String")
class FirString: FirElement

fun testAbstract(node: FirElement): Boolean {
    return when (node.getKind()) {
        ExprId.BinOp -> true
        else -> false
    }
}

fun testDirect(node: FirBinOp): Boolean {
    return when (node.getKind()) {
        ExprId.BinOp -> true
        else -> false
    }
}

fun box(): String {
    val node = FirBinOp()

    if (testAbstract(node) && testDirect(node)) {
        return "OK"
    } else {
        return "FAIL"
    }
}
