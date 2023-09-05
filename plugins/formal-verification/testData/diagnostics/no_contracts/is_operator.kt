fun <!VIPER_TEXT!>is_non_nullable<!>(x: Int?): Boolean {
    return x is Int
}

fun <!VIPER_TEXT!>not_is_nullable<!>(x: Int?): Boolean {
    return x !is Nothing
}

fun <!VIPER_TEXT!>smart_cast<!>(x: Any?): Int {
    if (x is Int) {
        return x
    } else {
        return -1
    }
}
