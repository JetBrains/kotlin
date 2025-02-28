// WITH_REFLECT
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
)

val property: Boolean = false

fun test1() {
    assert(::property.isOpen)
}

fun test2() {
    assert(::property.name == "a")
}

fun test3() {
    assert((::property)())
}