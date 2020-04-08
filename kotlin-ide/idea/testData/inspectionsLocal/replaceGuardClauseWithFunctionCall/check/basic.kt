// FIX: Replace with 'check()' call
// WITH_RUNTIME
fun test(b: Boolean) {
    <caret>if (b) throw IllegalStateException()
}