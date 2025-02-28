fun box(): String {
    return test1() +
            test2() +
            test3() +
            test4()
}

fun test1() = expectThrowableMessage {
    assert({ false }())
}

fun test2() = expectThrowableMessage {
    assert({ a: Int -> a > 10 }(9))
}

fun test3() = expectThrowableMessage {
    assert(fun1@ { a: Int -> a > 10 }(9))
}

fun test4() = expectThrowableMessage {
    assert(fun1@ { a: Int -> a > 10 }(9) == true)
}
