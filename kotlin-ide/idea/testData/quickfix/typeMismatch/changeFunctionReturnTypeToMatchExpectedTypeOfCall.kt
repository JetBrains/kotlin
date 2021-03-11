// "Change return type of called function 'bar' to 'String'" "true"
fun bar(): Any = ""
fun foo(): String = bar(<caret>)