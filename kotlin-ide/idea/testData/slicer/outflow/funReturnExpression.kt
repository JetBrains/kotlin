// FLOW: OUT

fun foo(n: Int): Int {
    return <caret>n
}

fun test() {
    val x = foo(1)
}