package source

sealed class Expr
data class Sum(val e1: Expr, val e2: Expr) : Expr()
object NotANumber : Expr()