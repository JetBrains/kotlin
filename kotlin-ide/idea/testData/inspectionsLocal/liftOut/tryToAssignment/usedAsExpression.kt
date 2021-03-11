// PROBLEM: none
var a = 5

fun foo() = <caret>try {
    a = 6
} catch (e: Exception) {
    a = 8
}