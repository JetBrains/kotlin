fun <!VIPER_TEXT!>use_nullable_twice<!>(x: Int?): Int? {
    val a = x
    val b = x
    return a
}

fun <!VIPER_TEXT!>pass_nullable_parameter<!>(x: Int?): Int? {
    use_nullable_twice(x)
    return x
}
