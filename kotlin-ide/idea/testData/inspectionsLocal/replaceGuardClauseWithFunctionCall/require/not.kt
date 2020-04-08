// FIX: Replace with 'require()' call
// WITH_RUNTIME
fun test(foo: Boolean) {
    <caret>if (!foo) throw IllegalArgumentException("test")
}