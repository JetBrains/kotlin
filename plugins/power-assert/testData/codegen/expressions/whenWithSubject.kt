// IGNORE_BACKEND_K1: ANY
fun box(): String {
    return test1(2, 2, 3) +
            test2() +
            test3() +
            test4(2, "2") +
            test5(2) +
            test6(2) +
            test7() +
            test8(2)
}

fun test1(x: Int, a: Int, b: Int) = expectThrowableMessage {
    assert(
        when (x) {
            b -> true
            a -> false
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
        when (a) {
            A.ONE -> false
            A.TWO -> true
        }
    )
}

fun test3() = expectThrowableMessage {
    val a: A = A.ONE
    assert(
        when (a) {
            A.ONE, A.TWO -> false
        }
    )
}

fun test4(x: Int, a: String) = expectThrowableMessage {
    assert(
        when (x) {
            a.toInt() -> false
            else -> true
        }
    )
}

fun test5(x: Int) = expectThrowableMessage {
    assert(
        when (x) {
            in 1..10 -> x == 3
            else -> true
        }
    )
}

fun test6(x: Any) = expectThrowableMessage {
    assert(
        when (x) {
            is Int -> x == 3
            else -> true
        }
    )
}

fun test7() = expectThrowableMessage {
    assert(
        when (val x = 2) {
            in 1..10 -> x == 3
            else -> true
        }
    )
}

fun test8(x: Any) = expectThrowableMessage {
    assert(
        when (x) {
            is Int if x == 2 -> false
            else -> true
        }
    )
}