fun box(): String {
    return test1(10) +
            test2(4) +
            test3(5) +
            test4(1) +
            test5(7) +
            test6(7) +
            test7(3) +
            test8(10)
}

fun test1(a: Int) = expectThrowableMessage {
    assert(a in 1..9)
}

fun test2(a: Int) = expectThrowableMessage {
    assert(a in 1..<4)
}

fun test3(a: Int) = expectThrowableMessage {
    assert(a in 4 downTo 1)
}

fun test4(a: Int) = expectThrowableMessage {
    assert(a in 0..8 step 2)
}

fun test5(a: Int) = expectThrowableMessage {
    assert(a in 0..<8 step 2)
}

fun test6(a: Int) = expectThrowableMessage {
    assert(a in 8 downTo 0 step 2)
}

fun test7(a: Int) = expectThrowableMessage {
    assert(a in (1..10).filter { it % 2 == 0 })
}

fun test8(a: Int) = expectThrowableMessage {
    assert(a in {a: Int -> a}(1)..{a: Int -> a}(5))
}