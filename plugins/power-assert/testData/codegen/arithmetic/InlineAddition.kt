fun box(): String {
    return test1() + test2(true, false)
}

fun test1() = expectThrowableMessage {
    assert(1 + 1 == 4)
}

fun test2(a: Boolean, b: Boolean) = expectThrowableMessage {
    assert(a + b)
}

operator fun Boolean.plus(b: Boolean) = this && b