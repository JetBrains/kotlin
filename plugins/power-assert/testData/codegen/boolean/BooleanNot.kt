fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
)

fun test1() {
    val b = true
    assert(!b)
}

fun test2() {
    val b = true
    assert(!b == true)
}