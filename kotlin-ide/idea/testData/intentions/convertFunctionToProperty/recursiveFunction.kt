// WITH_RUNTIME
fun String.<caret>foo(): String = if (isEmpty()) "" else substring(1).foo()