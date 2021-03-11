// WITH_RUNTIME
// FIX: Replace '==' with 'contentEquals'
fun foo() {
    val a = charArrayOf('a', 'b', 'c')
    val b = charArrayOf('a', 'b', 'c')
    if (a <caret>== b) {
    }
}
