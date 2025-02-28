fun box(): String {
    return test1() + test2(false, 1)
}

fun test1() = expectThrowableMessage {
    assert(2 / 1 == 4)
}

fun test2(a: Boolean, b: Int) = expectThrowableMessage {
    assert(a / b)
}

operator fun Boolean.div(b: Int): Boolean = this