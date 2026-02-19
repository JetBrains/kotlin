fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
)

operator fun Boolean.unaryPlus() = this

fun test1() {
    val b = false
    assert(+b)
}

fun test2() {
    val b = 2
    assert(+b == 3)
}