fun box(): String {
    return test1() +
            test2()
}

operator fun Boolean.unaryPlus() = this

fun test1() = expectThrowableMessage {
    val b = false
    assert(+b)
}

fun test2() = expectThrowableMessage {
    val b = 2
    assert(+b == 3)
}