// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
)

fun test1() {
    assert(fun(): Boolean { return false }())
}

fun test2() {
    assert(fun Int.(): Boolean { return this > 10 }(9))
}

fun test3() {
    assert(context(a: Int) fun(): Boolean { return a > 10 }(9))
}

fun test4()  {
    assert(fun1@ context(a: Int) fun(): Boolean { return a > 10 }(9))
}