// "Change type arguments to <*>" "false"
// ACTION: Convert to block body
// ACTION: Introduce local variable
fun <T> test(list: List<*>): List<T> = list as List<T><caret>