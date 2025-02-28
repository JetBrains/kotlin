fun box(): String {
    return test1(1) + test2(1) + test3(-1)
}

fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

fun test1(a: Int) = expectThrowableMessage {
    assert(IntPredicate { it < 0 }.accept(a))
}

fun test2(a: Int) = expectThrowableMessage {
    assert(IntPredicate { it < 0 }.accept(a) == true)
}

infix fun IntPredicate.plus(a: Int) = this.accept(a)

fun test3(a: Int) = expectThrowableMessage {
    assert(IntPredicate { it > 0 } plus a)
}
