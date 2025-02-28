fun box(): String = runAll(
    "test1" to { test1(A()) },
    "test2" to { test2(1, 2) },
    "test3" to { test3() },
    "test4" to { test4(1, 1, 2) },
)

class A {
    val a: Boolean? = false
    override fun toString(): String {
        return "A"
    }
}

fun test1(test: A?) {
    assert(foo@(test?.a ?: true))
}

fun test2(a: Int, b: Int) {
    assert(foo@(if (a < b) a == b else if (b < a) b == a else false))
}

fun test3() {
    assert(foo@(try { false } catch (e: Exception) { true }))
}

fun test4(x: Int, a: Int, b: Int) {
    assert(
        foo@(
            when {
                x == b -> true
                x == a -> false
                else -> true
            }
       )
    )
}