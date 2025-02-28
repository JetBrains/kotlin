// WITH_REFLECT
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
)

val <T> T.property: T
    get() = false as T

fun test1() {
    assert(Int::property.isOpen)
}

fun test2() {
    assert(Int::property.name == "a")
}

fun test3() {
    assert((Boolean::property)(false))
}

fun test4() {
    assert(Boolean::property.get(false))
}