// NEVER_VALIDATE

fun <!VIPER_TEXT!>isNonNullable<!>(x: Int?): Boolean {
    return x is Int
}

fun <!VIPER_TEXT!>notIsNullable<!>(x: Int?): Boolean {
    return x !is Nothing
}

fun <!VIPER_TEXT!>smartCast<!>(x: Any?): Int {
    if (x is Int) {
        return x
    } else {
        return -1
    }
}
