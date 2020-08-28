// FIR_COMPARISON
interface Expr
class Sum(val left : Expr, val right : Expr) : Expr

fun evalWhen(e : Expr) : Int = when (e) {
    is Sum -> e.<caret>
}

// EXIST: left, right