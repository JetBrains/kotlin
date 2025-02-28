fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
)

fun test1() {
    var i = 3
    assert(--i == 4)
}

fun test2() {
    var a = true
    assert(--a)
}

operator fun Boolean.dec() = !this