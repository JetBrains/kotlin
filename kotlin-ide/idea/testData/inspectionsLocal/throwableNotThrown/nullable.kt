// PROBLEM: none
fun foo(): RuntimeException? {
    return RuntimeException()
}

fun test() {
    <caret>foo()
}