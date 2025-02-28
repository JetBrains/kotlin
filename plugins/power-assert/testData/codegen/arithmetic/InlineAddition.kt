fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2(true, false) },
)

fun test1() {
    assert(1 + 1 == 4)
}

fun test2(a: Boolean, b: Boolean) {
    assert(a + b)
}

operator fun Boolean.plus(b: Boolean) = this && b