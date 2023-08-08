fun <!VIPER_TEXT!>return_unit<!>() {}
fun <!VIPER_TEXT!>return_int<!>(): Int { return 0 }
fun <!VIPER_TEXT!>take_int_return_unit<!>(@Suppress("UNUSED_PARAMETER") x: Int) {}
fun <!VIPER_TEXT!>take_int_return_int<!>(x: Int): Int { return x }
fun <!VIPER_TEXT!>take_int_return_int_expr<!>(x: Int): Int = x
fun <!VIPER_TEXT!>with_int_declaration<!>(): Int {
    val x = 0
    return x
}
fun <!VIPER_TEXT!>while_loop<!>(b: Boolean): Int {
    while (b) {
        val a = 1
        val c = 2
    }
    return 0
}
fun <!VIPER_TEXT!>simple_if<!>(): Int {
    if (true) {
        return 0
    } else {
        return 1
    }
}
fun <!VIPER_TEXT!>if_on_parameter<!>(b: Boolean): Int {
    if (b) {
        return 0
    } else {
        return 1
    }
}
fun <!VIPER_TEXT!>function_call<!>() {
    return_unit()
    return_unit()
}
fun <!VIPER_TEXT!>function_call_nested<!>() {
    take_int_return_unit(take_int_return_int(return_int()))
}