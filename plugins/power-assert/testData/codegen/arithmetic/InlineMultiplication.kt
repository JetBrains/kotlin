fun box(): String {
    return test1() + test2(false, 2)
}

fun test1() = expectThrowableMessage {
    assert(1 * 2 == 4)
}

fun test2(a: Boolean, b: Int) = expectThrowableMessage {
    assert(a * b)
}

operator fun Boolean.times(b: Int): Boolean = this
