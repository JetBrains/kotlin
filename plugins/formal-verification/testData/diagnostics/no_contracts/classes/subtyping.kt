// NEVER_VALIDATE

import org.jetbrains.kotlin.formver.plugin.NeverConvert

fun <!VIPER_TEXT!>smartCast<!>(x: Int?): Int {
    if (x == null) {
        return 0
    } else {
        return x
    }
}

fun <!VIPER_TEXT!>returnSubtyping<!>(): Int? {
    return 0
}

fun <!VIPER_TEXT!>assignmentSubtyping<!>() {
    var x: Boolean? = false
    x = true
}

@NeverConvert
fun nullableParameter(b: Boolean?) {}

fun <!VIPER_TEXT!>functionParameterSubtyping<!>() {
    nullableParameter(false)
}
