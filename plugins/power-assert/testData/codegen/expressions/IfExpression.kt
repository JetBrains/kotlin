// DUMP_KT_IR

fun box(): String {
    return test1(2, 1) +
            test1(1, 2) +
            test1(1, 1) +
            test2(1, 2) +
            test2(2, 1) +
            test3(2, 1) +
            test3(1, 2) +
            test3(1, 1) +
            test4(true, 2, 1) +
            test4(true, 1, 2) +
            test4(true, 1, 1) +
            test4(false, 1, 1)
}

fun test1(a: Int, b: Int) = expectThrowableMessage {
    assert(if (a < b) a == b else if (b < a) b == a else false)
}

fun test2(a: Int, b: Int) = expectThrowableMessage {
    assert(a + (if (a < b) a else b) + b == a)
}

fun test3(a: Int, b: Int) = expectThrowableMessage {
    assert(
        when {
            a < b -> a == b
            b < a -> b == a
            else -> false
        }
    )
}

fun test4(initial: Boolean, a: Int, b: Int) = expectThrowableMessage {
    assert(
        initial && when {
            a < b -> a == b
            b < a -> b == a
            else -> false
        }
    )
}
