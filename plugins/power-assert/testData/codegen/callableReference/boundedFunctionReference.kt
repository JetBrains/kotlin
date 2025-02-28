// WITH_REFLECT

fun box(): String {
    return test1() +
            test2() +
            test3() +
            test4() +
            test5() +
            test6()
}
class A {
    fun foo(x: Int): Boolean = x % 2 != 0
    override fun toString(): String {
        return "A"
    }
}

fun List<Int>.bar(x: Int): Boolean {
    return false
}

fun test1() = expectThrowableMessage {
    assert(A::foo == { x: Int -> x % 2 != 0 })
}

fun test2() = expectThrowableMessage {
    assert(A::foo.isInfix)
}

fun test3() = expectThrowableMessage {
    assert((A::foo)(A(), 4))
}

fun test4() = expectThrowableMessage {
    assert(List<Int>::bar.name == "foo")
}

fun test5() = expectThrowableMessage {
    assert(List<Int>::bar.isInfix)
}

fun test6() = expectThrowableMessage {
    assert((List<Int>::bar)(listOf(1,2,3), 1))
}