fun box(): String {
    return test1(2, 2, 3) +
            test2() +
            test3() +
            test4(2, "2") +
            test5(2) +
            test6(2) +
            test7(Pair(1,2))
}

fun test1(x: Int, a: Int, b: Int) = expectThrowableMessage {
    assert(
        when {
            x == b -> true
            x == a -> false
            else -> true
        }
    )
}

enum class A {
    ONE, TWO
}

fun test2() = expectThrowableMessage {
    val a: A = A.ONE
    assert(
        when {
            a == A.ONE -> false
            a == A.TWO -> true
            else -> true
        }
    )
}

fun test3() = expectThrowableMessage {
    val a: A = A.ONE
    assert(
        when {
            a == A.ONE || a == A.TWO -> false
            else -> true
        }
    )
}

fun test4(x: Int, a: String) = expectThrowableMessage {
    assert(
        when {
            x == a.toInt() -> false
            else -> true
        }
    )
}

fun test5(x: Int) = expectThrowableMessage {
    assert(
        when {
            x in 1..10 -> x == 3
            else -> true
        }
    )
}

fun test6(x: Any) = expectThrowableMessage {
    assert(
        when {
            x is Int -> x == 3
            else -> true
        }
    )
}

fun test7(t: Pair<Int, Int>) = expectThrowableMessage {
    assert(
        when {
            t == Pair(10, 10) -> true
            else -> false
        }
    )
}


