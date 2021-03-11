// "Change type arguments to <*, *>" "true"
fun test(a: Any) {
    (a as Map<Int, Boolean><caret>).bar()
}

fun <T> Map<T, *>.bar() {}