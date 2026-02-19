val p1 = "O"
val p2 = "K"
val pp = p1 + p2

fun bar(): String {
    val v = pp
    @Suppress("JSCODE_ARGUMENT_NON_CONST_EXPRESSION")
    val b = js("\"$v\"")
    return b
}

fun box(): String = bar()
