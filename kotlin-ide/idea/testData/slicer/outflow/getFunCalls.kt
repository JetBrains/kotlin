// FLOW: OUT

class A {
    operator fun <caret>get(n: Int) = this
}

fun test() {
    val x = A()[2]
}