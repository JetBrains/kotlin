// FLOW: OUT

fun foo(n: Int): Int = <caret>n

fun test() {
    val x = foo(1)
}