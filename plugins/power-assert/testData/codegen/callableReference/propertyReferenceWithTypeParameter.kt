// WITH_REFLECT
fun box(): String {
    return test1() +
            test2() +
            test3() +
            test4()
}

val <T> T.property: T
    get() = false as T

fun test1() = expectThrowableMessage {
    assert(Int::property.isOpen)
}

fun test2() = expectThrowableMessage {
    assert(Int::property.name == "a")
}

fun test3() = expectThrowableMessage {
    assert((Boolean::property)(false))
}

fun test4() = expectThrowableMessage {
    assert(Boolean::property.get(false))
}