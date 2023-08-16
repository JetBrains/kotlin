fun <!VIPER_TEXT!>negation<!>(x: Boolean): Boolean {
    return !x
}
fun <!VIPER_TEXT!>conjunction<!>(x: Boolean, y: Boolean): Boolean {
    return x && y
}
fun <!VIPER_TEXT!>conjunction_side_effects<!>(x: Boolean, y: Boolean): Boolean {
    // This does not actually have side effects, but the code should compile as if it might.
    return negation(x) && negation(y)
}
fun <!VIPER_TEXT!>disjunction<!>(x: Boolean, y: Boolean): Boolean {
    return x || y
}
