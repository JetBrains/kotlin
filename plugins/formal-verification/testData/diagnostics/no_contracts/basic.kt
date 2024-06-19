// NEVER_VALIDATE

fun <!VIPER_TEXT!>returnUnit<!>() {}
fun <!VIPER_TEXT!>returnInt<!>(): Int { return 0 }
fun <!VIPER_TEXT!>takeIntReturnUnit<!>(@Suppress("UNUSED_PARAMETER") x: Int) {}
fun <!VIPER_TEXT!>takeIntReturnInt<!>(x: Int): Int { return x }
fun <!VIPER_TEXT!>takeIntReturnIntExpr<!>(x: Int): Int = x
fun <!VIPER_TEXT!>withIntDeclaration<!>(): Int {
    val x = 0
    return x
}
fun <!VIPER_TEXT!>intAssignment<!>() {
    var x = 0
    x = 1
}