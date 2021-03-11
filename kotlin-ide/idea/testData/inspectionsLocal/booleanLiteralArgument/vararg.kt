// PROBLEM: none
fun foo(vararg b: Boolean) {}

fun test() {
    foo(true, true, true<caret>)
}