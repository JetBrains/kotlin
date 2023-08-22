fun <!VIPER_TEXT!>unit_function<!>(@Suppress("UNUSED_PARAMETER") f: () -> Unit) {
}

fun <!VIPER_TEXT!>function_object_call<!>(g: (Boolean, Int) -> Int): Int {
    return g(true, 0)
}

fun <!VIPER_TEXT!>function_object_nested_call<!>(f: (Int) -> Int, g: (Boolean) -> Int): Int {
    return f(f(g(false)))
}