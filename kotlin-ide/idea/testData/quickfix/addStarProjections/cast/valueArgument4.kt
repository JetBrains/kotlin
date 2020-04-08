// "Change type arguments to <*>" "true"
fun test(a: Any) {
    foo(a as List<Boolean><caret>)
}

fun <T> foo(list: List<T>) {}