fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2(false, 1) },
)

fun test1() {
    assert(2 / 1 == 4)
}

fun test2(a: Boolean, b: Int) {
    assert(a / b)
}

operator fun Boolean.div(b: Int): Boolean = this