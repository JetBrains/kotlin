fun box(): String {
    return test1() +
            test2(0b10011, 0b11110) +
            test3() +
            test4() +
            test5(null)

}
fun test1() = expectThrowableMessage {
    assert(0b110011 shl 2 == 0b11011100)
}

fun test2(a: Int, b: Int) = expectThrowableMessage {
    assert(a shr b == 0b10110)
}

fun test3() = expectThrowableMessage {
    assert(-0b1100110011 ushr 22 == 0b1111110111)
}

fun test4() = expectThrowableMessage {
    assert(5 shl 3 + 4 shr 1 == 9)
}

fun test5(x: Int?) = expectThrowableMessage {
    assert(x ?: 2 shl 1 == 1)
}