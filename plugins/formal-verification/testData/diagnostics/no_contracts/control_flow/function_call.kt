// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

@NeverConvert
fun f(x: Int): Int = x

fun <!VIPER_TEXT!>functionCall<!>() {
    f(0)
    f(0)
}

fun <!VIPER_TEXT!>functionCallNested<!>() {
    f(f(f(0)))
}
