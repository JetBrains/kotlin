// PROBLEM: 'do while' has empty body
// FIX: none

fun test(i: Int) {
    <caret>do {
    } while (i == 1)
}