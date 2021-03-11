// FLOW: OUT
fun foo(n: Int) {
    val y = n
}

fun test() {
    val x = <caret>1

    foo(x)
}