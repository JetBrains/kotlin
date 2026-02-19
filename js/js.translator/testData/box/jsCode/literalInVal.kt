fun foo(): String {
    val q1 = "O"
    val q2 = "K"
    val qq = q1 + q2
    @Suppress("JSCODE_ARGUMENT_NON_CONST_EXPRESSION")
    val b = js("\"$qq\"")
    return b
}



fun box() = foo()
