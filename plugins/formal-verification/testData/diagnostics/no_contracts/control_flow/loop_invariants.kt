// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
fun returnsBoolean(): Boolean {
    return false
}

fun <!VIPER_TEXT!>dynamicLambdaInvariant<!>(f: () -> Int) {
    while (returnsBoolean()) {
        f()
    }
}

fun <!VIPER_TEXT!>functionAssignment<!>(f: () -> Int) {
    val g = f
    while (returnsBoolean()) {
        g()
    }
}

fun <!VIPER_TEXT!>conditionalFunctionAssignment<!>(b: Boolean, f: () -> Int, h: () -> Int) {
    val g = if (b) f else h
    while (returnsBoolean()) {
        g()
    }
}