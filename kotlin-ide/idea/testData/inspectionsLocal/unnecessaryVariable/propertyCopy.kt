// PROBLEM: none

class My(val x: Number)

fun My.foo(): Int {
    val <caret>y = x
    if (y is Int) return y
    return 0
}