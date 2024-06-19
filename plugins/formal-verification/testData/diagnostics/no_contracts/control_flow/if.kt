// NEVER_VALIDATE

fun <!VIPER_TEXT!>simpleIf<!>(): Int {
    if (true) {
        return 0
    } else {
        return 1
    }
}
fun <!VIPER_TEXT!>ifOnParameter<!>(b: Boolean): Int {
    if (b) {
        return 0
    } else {
        return 1
    }
}
fun <!VIPER_TEXT!>ifAsExpression<!>(): Boolean {
    var b = false
    // Including side effects so that we can see the sequencing is correct.
    return if (b) {
        simpleIf()
        false
    } else {
        ifOnParameter(b)
        true
    }
}