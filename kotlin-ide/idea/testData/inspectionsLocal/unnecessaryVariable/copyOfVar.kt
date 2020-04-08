// PROBLEM: none

fun test(): Int {
    var x = 1
    val <caret>y = x
    x++
    return x + y
}