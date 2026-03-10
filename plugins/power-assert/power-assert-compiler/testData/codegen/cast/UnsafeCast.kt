// WITH_REFLECT
fun box(): String = runAll(
    "test1" to { test1("test")  },
    "test2" to { test2("test") },
    "test3" to { test3<Int>(listOf(1, 2)) },
    "test4" to { test4(listOf(1, 2)) },
    "test5" to { test5("test") },
    "test6" to { test6(null) },
    "test7" to { test7(mutableListOf(1, 2)) },
    "test8" to { test8(Base()) },
    "test9" to { test9() },
)

fun test1(a: Any) {
    assert((a as String).length == 5)
}

fun test2(a: Any) {
    assert((a as String?)?.length == 5)
}

fun <T> test3(a: Any) {
    assert((a as List<T>).isEmpty())
}

fun test4(a: Any) {
    assert((a as List<*>).isEmpty())
}

typealias MyString = String

fun test5(a: Any) {
    assert((a as MyString).length == 5)
}

fun test6(a: Any?) {
    assert(a as Nothing? == "null")
}

fun test7(a: MutableCollection<Int>) {
    assert((a as MutableList<Int>).add(3) == false)
}

class Base {
    override fun toString(): String {
        return "Base"
    }
}
operator fun Base.plus(arg: Int) = false

fun test8(a: Any) {
    assert(a as Base + 1)
}

fun foo() = false

fun test9() {
    assert((::foo as () -> Boolean)())
}


