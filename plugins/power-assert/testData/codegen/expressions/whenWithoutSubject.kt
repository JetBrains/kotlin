fun box(): String = runAll(
    "test1" to { test1(2, 2, 3) },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4(2, "2") },
    "test5" to { test5(2) },
    "test6" to { test6(2) },
    "test7" to { test7(Pair(1,2)) },
)

fun test1(x: Int, a: Int, b: Int) {
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

fun test2() {
    val a: A = A.ONE
    assert(
        when {
            a == A.ONE -> false
            a == A.TWO -> true
            else -> true
        }
    )
}

fun test3() {
    val a: A = A.ONE
    assert(
        when {
            a == A.ONE || a == A.TWO -> false
            else -> true
        }
    )
}

fun test4(x: Int, a: String) {
    assert(
        when {
            x == a.toInt() -> false
            else -> true
        }
    )
}

fun test5(x: Int) {
    assert(
        when {
            x in 1..10 -> x == 3
            else -> true
        }
    )
}

fun test6(x: Any) {
    assert(
        when {
            x is Int -> x == 3
            else -> true
        }
    )
}

fun test7(t: Pair<Int, Int>) {
    assert(
        when {
            t == Pair(10, 10) -> true
            else -> false
        }
    )
}


