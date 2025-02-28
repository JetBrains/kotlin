fun box(): String {
    return test1() + test2()
}

fun test1() = expectThrowableMessage {
    var i = 3
    assert(--i == 4)
}

fun test2() = expectThrowableMessage {
    var a = true
    assert(--a)
}

operator fun Boolean.dec() = !this