package source

sealed class <caret>Expr
data class <caret>Const(val number: Double) : Expr()
data class <caret>Sum(val e1: Expr, val e2: Expr) : Expr()
object <caret>NotANumber : Expr()