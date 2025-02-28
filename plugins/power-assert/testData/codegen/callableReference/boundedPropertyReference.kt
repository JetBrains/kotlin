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
    val foo: Boolean = false
    override fun toString(): String {
        return "A"
    }
}

val List<Int>.bar: Boolean
    get() {
        return false
    }

fun test1() = expectThrowableMessage {
    assert(A::foo == { true })
}

fun test2() = expectThrowableMessage {
    assert(A::foo.isOpen)
}

fun test3() = expectThrowableMessage {
    assert((A::foo)(A()))
}

fun test4() = expectThrowableMessage {
    assert(List<Int>::bar.name == "foo")
}

fun test5() = expectThrowableMessage {
    assert(List<Int>::bar.isConst)
}

fun test6() = expectThrowableMessage {
    assert((List<Int>::bar)(listOf(1,2,3)))
}