fun <!VIPER_TEXT!>use_nullable_twice<!>(x: Int?): Int? {
    val a = x
    val b = x
    return a
}

fun <!VIPER_TEXT!>pass_nullable_parameter<!>(x: Int?): Int? {
    use_nullable_twice(x)
    return x
}

fun <!VIPER_TEXT!>nullable_nullable_comparison<!>(x: Int?, y: Int?): Boolean {
    return x == y
}

fun <!VIPER_TEXT!>nullable_non_nullable_comparison<!>(x: Int?, y: Int?): Boolean {
    return x != 3
}

fun <!VIPER_TEXT!>null_comparison<!>(x: Int?): Boolean {
    return x == null
}