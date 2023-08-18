fun <!VIPER_TEXT!>simple_if<!>(): Int {
    if (true) {
        return 0
    } else {
        return 1
    }
}
fun <!VIPER_TEXT!>if_on_parameter<!>(b: Boolean): Int {
    if (b) {
        return 0
    } else {
        return 1
    }
}
fun <!VIPER_TEXT!>if_as_expression<!>(): Boolean {
    var b = false
    // Including side effects so that we can see the sequencing is correct.
    return if (b) {
        simple_if()
        false
    } else {
        if_on_parameter(b)
        true
    }
}