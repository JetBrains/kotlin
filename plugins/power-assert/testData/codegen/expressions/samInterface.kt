fun box(): String = runAll(
    "test1" to { test1(1) },
    "test2" to { test2(1) },
    "test3" to { test3(-1) },
)

fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

fun test1(a: Int) {
    assert(IntPredicate { it < 0 }.accept(a))
}

fun test2(a: Int) {
    assert(IntPredicate { it < 0 }.accept(a) == true)
}

infix fun IntPredicate.plus(a: Int) = this.accept(a)

fun test3(a: Int) {
    assert(IntPredicate { it > 0 } plus a)
}
