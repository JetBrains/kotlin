// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun box(): String {
    return test1() +
            test2() +
            test3() +
            test4()
}

fun test1() = expectThrowableMessage {
    assert(fun(): Boolean { return false }())
}

fun test2() = expectThrowableMessage {
    assert(fun Int.(): Boolean { return this > 10 }(9))
}

fun test3() = expectThrowableMessage {
    assert(context(a: Int) fun(): Boolean { return a > 10 }(9))
}

fun test4() = expectThrowableMessage {
    assert(fun1@ context(a: Int) fun(): Boolean { return a > 10 }(9))
}