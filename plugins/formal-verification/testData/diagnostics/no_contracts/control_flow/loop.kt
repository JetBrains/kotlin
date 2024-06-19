// NEVER_VALIDATE

fun <!VIPER_TEXT!>whileLoop<!>(b: Boolean): Boolean {
    while (b) {
        val a = 1
        val c = 2
    }
    return false
}

fun <!VIPER_TEXT!>whileFunctionCondition<!>() {
    while (whileLoop(true)) { }
}