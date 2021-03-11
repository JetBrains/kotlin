// WITH_RUNTIME
fun test(b: Boolean): Int {
    <caret>return if (b) {
        1
    } else {
        TODO()
    }
}