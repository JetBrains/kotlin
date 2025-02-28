fun box(): String {
    return test1() +
            test2(0b10011, 0b11110) +
            test3(0b10011, 0b11110) +
            test4(0b10011, 0b11110) +
            test5() +
            test6(null)

}
fun test1() = expectThrowableMessage {
    assert(true and false)
}

fun test2(a: Int, b: Int) = expectThrowableMessage {
    assert(a and b == 0b10110)
}

fun test3(a: Int, b: Int) = expectThrowableMessage {
    assert(a or b == 0b10110)
}

fun test4(a: Int, b: Int) = expectThrowableMessage {
    assert(a xor b == 0b10110)
}

fun test5() = expectThrowableMessage {
    assert(5 and 3 + 4 or 1 == 9)
}

fun test6(x: Int?) = expectThrowableMessage {
    assert(x ?: 2 and 1 == 1)
}