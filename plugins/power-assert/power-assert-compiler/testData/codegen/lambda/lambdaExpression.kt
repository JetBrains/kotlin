fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
)

fun test1() {
    assert({ false }())
}

fun test2() {
    assert({ a: Int -> a > 10 }(9))
}

fun test3() {
    assert(fun1@ { a: Int -> a > 10 }(9))
}

fun test4() {
    assert(fun1@ { a: Int -> a > 10 }(9) == true)
}
