// WITH_REFLECT
fun box(): String {
    return test1() +
            test2() +
            test3()
}

val property: Boolean = false

fun test1() = expectThrowableMessage {
    assert(::property.isOpen)
}

fun test2() = expectThrowableMessage {
    assert(::property.name == "a")
}

fun test3() = expectThrowableMessage {
    assert((::property)())
}