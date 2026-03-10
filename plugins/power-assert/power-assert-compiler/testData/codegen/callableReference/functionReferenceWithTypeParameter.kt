// WITH_REFLECT
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3<Int>() },
    "test4" to { test4() },
)

fun <T> foo(): T { return false as T }

fun <T> List<T>.bar(): Boolean { return false }

fun test1() {
    val ref: () -> Boolean = ::foo
    assert(ref())
}

fun test2() {
    assert(List<Int>::bar.name == "foo")
}

fun <T> test3() {
    assert(List<T>::bar.isInfix)
}

fun test4() {
    assert((List<Int>::bar)(listOf()))
}