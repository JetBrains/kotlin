// IS_APPLICABLE: false
fun foo() {
    val a = true
    val b = false
    if (a) {
        val v = a <caret>&& b
    }
}