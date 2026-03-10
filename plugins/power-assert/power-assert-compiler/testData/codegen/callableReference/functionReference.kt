// WITH_REFLECT
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
)

fun foo(x: Int) : Boolean = false

fun test1() {
    assert(::foo.name == "bar")
}

fun test2() {
    assert(::foo.isOpen)
}

fun test3() {
    assert((::foo)(1))
}
