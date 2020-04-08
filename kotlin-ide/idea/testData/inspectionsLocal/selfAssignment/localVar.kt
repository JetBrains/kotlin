// PROBLEM: Variable 'bar' is assigned to itself
// FIX: Remove self assignment

fun test() {
    var bar = 1
    bar = <caret>bar
}