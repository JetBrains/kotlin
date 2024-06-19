// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

fun <!VIPER_TEXT!>useNullableTwice<!>(x: Int?): Int? {
    val a = x
    val b = x
    return a
}

fun <!VIPER_TEXT!>passNullableParameter<!>(x: Int?): Int? {
    useNullableTwice(x)
    return x
}

fun <!VIPER_TEXT!>nullableNullableComparison<!>(x: Int?, y: Int?): Boolean {
    return x == y
}

fun <!VIPER_TEXT!>nullableNonNullableComparison<!>(x: Int?, y: Int?): Boolean {
    return x != 3
}

fun <!VIPER_TEXT!>nullComparison<!>(x: Int?): Boolean {
    return x == null
}
