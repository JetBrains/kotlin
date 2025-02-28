// WITH_REFLECT
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
)

data class Person(val name: String, val isAlive: Boolean)

fun test1() {
    assert((::Person)("Alice", true).name == "Kate")
}

fun test2() {
    assert((::Person)("Alice", false).isAlive)
}

fun test3() {
    assert(::Person.isSuspend)
}