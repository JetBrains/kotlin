// "Add 'return' to last expression" "false"
// ERROR: A 'return' expression required in a function with a block body ('{...}')
// ACTION: Introduce local variable
// ACTION: Remove explicitly specified return type of enclosing function 'test'

fun test(): Boolean {
    5
}<caret>
