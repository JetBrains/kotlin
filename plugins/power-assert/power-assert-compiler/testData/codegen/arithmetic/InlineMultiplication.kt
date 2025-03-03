fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2(false, 2) },
)

fun test1() {
    assert(1 * 2 == 4)
}

fun test2(a: Boolean, b: Int) {
    assert(a * b)
}

operator fun Boolean.times(b: Int): Boolean = this
