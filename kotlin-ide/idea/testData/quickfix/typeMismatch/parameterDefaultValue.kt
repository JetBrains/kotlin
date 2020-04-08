// "Change parameter 's' type of function 'foo' to 'Int'" "true"
val ONE = 1

fun foo(s: String = <caret>ONE + 1) {}