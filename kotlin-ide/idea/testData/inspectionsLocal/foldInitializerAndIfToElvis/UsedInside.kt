// PROBLEM: none

interface A {
    val s: String
}

fun foo() = Any()

fun test(): String {
    val y = foo()
    <caret>if (y !is A) return y.toString()
    return y.s
}