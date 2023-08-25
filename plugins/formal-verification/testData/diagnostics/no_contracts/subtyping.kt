fun <!VIPER_TEXT!>smart_cast<!>(x: Int?): Int {
    if (x == null) {
        return 0
    } else {
        return x
    }
}

fun <!VIPER_TEXT!>return_subtyping<!>(): Int? {
    return 0
}

fun <!VIPER_TEXT!>assignment_subtyping<!>() {
    var x: Boolean? = false
    x = true
}

fun <!VIPER_TEXT!>nullable_parameter<!>(b: Boolean?) {}

fun <!VIPER_TEXT!>function_parameter_subtyping<!>() {
    nullable_parameter(false)
}