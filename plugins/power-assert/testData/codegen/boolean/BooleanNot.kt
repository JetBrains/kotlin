fun box(): String {
    return test1() +
            test2()
}

fun test1() = expectThrowableMessage {
    val b = true
    assert(!b)
}

fun test2() = expectThrowableMessage {
    val b = true
    assert(!b == true)
}