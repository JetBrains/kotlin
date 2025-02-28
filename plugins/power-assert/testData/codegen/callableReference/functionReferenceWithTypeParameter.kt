// WITH_REFLECT
fun box(): String {
    return test1() +
            test2() +
            test3<Int>()
}

fun <T> foo(): T { return false as T }

fun <T> List<T>.bar(): Boolean { return false }

fun test1() = expectThrowableMessage {
    val ref: () -> Boolean = ::foo
    assert(ref())
}

fun test2() = expectThrowableMessage {
    assert(List<Int>::bar.name == "foo")
}

fun <T> test3() = expectThrowableMessage {
    assert(List<T>::bar.isInfix)
}

fun test4() = expectThrowableMessage {
    assert((List<Int>::bar)(listOf()))
}