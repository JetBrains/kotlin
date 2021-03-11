// HIGHLIGHT: GENERIC_ERROR_OR_WARNING
// FIX: Add 'c =' to argument
fun foo(a: Boolean, b: Boolean, c: Boolean) {}

fun test() {
    foo(true, true, true<caret>)
}