fun box(): String = runAll(
    "test1(2, 1)" to { test1(2, 1) },
    "test1(1, 2)" to { test1(1, 2) },
    "test2(1, 2)" to { test2(1, 2) },
    "test3(1, 2)" to { test3(1, 2) },
)

fun test1(a: Int, b: Int) {
    assert(if (a > b) { a == b } else { a.inc() == 3 })
}

fun test2(a: Int, b: Int) {
    assert(if (a < b) { if (a > 10) true ; false } else { true })
}

fun test3(a: Int, b: Int) {
    assert(if (a < b) { if (a > 10) true else false } else false)
}
