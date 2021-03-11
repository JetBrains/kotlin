// "Create type parameter in class 'X'" "false"
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Introduce import alias
// ERROR: No type arguments expected for class X

class X

fun foo(x: <caret>X<String>) {}