// "Remove parameter 'block'" "true"
fun doNotUse1(<caret>block: () -> Unit) {}

fun useNotUse1() = doNotUse1 { }
