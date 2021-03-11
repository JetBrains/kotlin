// "Change type arguments to <*>" "true"
fun test(a: Any) {
    (a as List<Boolean><caret>).bar()
}

fun List<*>.bar() {}