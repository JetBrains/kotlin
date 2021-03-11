// IS_APPLICABLE: false
fun test(b: Boolean): String {
    if (b == true) {
        if (true) return "first"
    }<caret>
    else if (b == false) {
        if (true) return "second"
    }

    return "none"
}