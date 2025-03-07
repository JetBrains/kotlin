// WITH_REFLECT
fun box(): String {
    return test1("test") +
            test2("test") +
            test3<Int>(listOf(1, 2)) +
            test4(listOf(1, 2)) +
            test5("test") +
            test6(null) +
            test7(mutableListOf(1, 2)) +
            test8(Base()) +
            test9()
}

fun test1(a: Any) = expectThrowableMessage {
    assert((a as String).length == 5)
}

fun test2(a: Any) = expectThrowableMessage {
    assert((a as String?)?.length == 5)
}

fun <T> test3(a: Any) = expectThrowableMessage {
    assert((a as List<T>).isEmpty())
}

fun test4(a: Any) = expectThrowableMessage {
    assert((a as List<*>).isEmpty())
}

typealias MyString = String

fun test5(a: Any) = expectThrowableMessage {
    assert((a as MyString).length == 5)
}

fun test6(a: Any?) = expectThrowableMessage {
    assert(a as Nothing? == "null")
}

fun test7(a: MutableCollection<Int>) = expectThrowableMessage {
    assert((a as MutableList<Int>).add(3) == false)
}

class Base {
    override fun toString(): String {
        return "Base"
    }
}
operator fun Base.plus(arg: Int) = false

fun test8(a: Any) = expectThrowableMessage {
    assert(a as Base + 1)
}

fun foo() = false

fun test9() = expectThrowableMessage {
    assert((::foo as () -> Boolean)())
}


