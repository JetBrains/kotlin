// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
fun id(x: Int?): Int? = x

fun <!VIPER_TEXT!>elvisOperator<!>(x: Int?): Int {
    return x ?: 3
}

fun <!VIPER_TEXT!>elvisOperatorComplex<!>(x: Int?): Int {
    return id(x) ?: elvisOperator(2)
}

fun <!VIPER_TEXT!>elvisOperatorReturn<!>(x: Int?): Int {
    val y = x ?: return 0
    return y
}

