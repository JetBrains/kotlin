// PROBLEM: none
fun foo() {
    throw RuntimeException()
}

fun test() {
    <caret>foo()
}