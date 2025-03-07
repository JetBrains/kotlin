// WITH_REFLECT
fun box(): String {
    return test1() +
            test2() +
            test3()
}

data class Person(val name: String, val isAlive: Boolean)

fun test1() = expectThrowableMessage {
    assert((::Person)("Alice", true).name == "Kate")
}

fun test2() = expectThrowableMessage {
    assert((::Person)("Alice", false).isAlive)
}

fun test3() = expectThrowableMessage {
    assert(::Person.isSuspend)
}