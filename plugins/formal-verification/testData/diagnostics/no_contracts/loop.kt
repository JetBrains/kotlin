fun <!VIPER_TEXT!>while_loop<!>(b: Boolean): Boolean {
    while (b) {
        val a = 1
        val c = 2
    }
    return false
}

fun <!VIPER_TEXT!>while_function_condition<!>() {
    while (while_loop(true)) { }
}
