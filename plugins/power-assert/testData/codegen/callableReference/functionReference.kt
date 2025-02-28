// WITH_REFLECT

fun box(): String {
    return test1() +
            test2() +
            test3()
}

fun foo(x: Int) : Boolean = false

fun test1() = expectThrowableMessage {
    assert(::foo.name == "bar")
}

fun test2() = expectThrowableMessage {
    assert(::foo.isOpen)
}

fun test3() = expectThrowableMessage {
    assert((::foo)(1))
}
