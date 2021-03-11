// "Change type arguments to <*, *>" "false"
fun test(a: Any) {
    (a as Map<Int, Boolean><caret>).bar()
}

fun Map<Int, Boolean>.bar() {}