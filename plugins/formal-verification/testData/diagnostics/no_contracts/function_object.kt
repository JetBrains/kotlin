// NEVER_VALIDATE

fun <!VIPER_TEXT!>unitFunction<!>(@Suppress("UNUSED_PARAMETER") f: () -> Unit) {
}

fun <!VIPER_TEXT!>functionObjectCall<!>(g: (Boolean, Int) -> Int): Int {
    return g(true, 0)
}

fun <!VIPER_TEXT!>functionObjectNestedCall<!>(f: (Int) -> Int, g: (Boolean) -> Int): Int {
    return f(f(g(false)))
}