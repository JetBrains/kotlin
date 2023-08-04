fun <!VIPER_TEXT!>return_unit<!>() {}
fun <!VIPER_TEXT!>return_int<!>(): Int { return 0 }
fun <!VIPER_TEXT!>take_int_return_unit<!>(@Suppress("UNUSED_PARAMETER") x: Int) {}
fun <!VIPER_TEXT!>take_int_return_int<!>(x: Int): Int { return x }
fun <!VIPER_TEXT!>take_int_return_int_expr<!>(x: Int): Int = x
fun <!VIPER_TEXT!>with_int_declaration<!>(): Int {
    val x = 0
    return x
}
