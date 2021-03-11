// "Change 'y' to '*y'" "false"
// ACTION: Add 'toString()' call
// ACTION: Change parameter 'x' type of function 'foo' to 'Array<Int>'
// ACTION: Convert to block body
// ACTION: Create function 'foo'
// DISABLE-ERRORS

fun foo(vararg x: String) {}

fun bar(y: Array<Int>) = foo(y<caret>)
