// FIX: Replace with 'require()' call
// WITH_RUNTIME
fun test(foo: Int) {
    <caret>if (foo != 0) throw IllegalArgumentException("test")
}