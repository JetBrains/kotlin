// PROBLEM: none
fun foo(a: Boolean, b: Boolean, c: Boolean) {}

fun test() {
    foo(true, true, c = true<caret>)
}