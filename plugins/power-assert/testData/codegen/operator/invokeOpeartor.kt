// WITH_REFLECT
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
    "test5" to { test5() },
    "test6" to { test6() },
)

operator fun Any.invoke(): Boolean { return false }

fun test1() {
    val stringValue = "true"
    assert(stringValue())
}

class Callable {
    operator fun invoke(): String = "Hello"
    operator fun invoke(a: List<Int>): String = "Hello"
    operator fun invoke(b: Int, a: List<Int>): String = "Hello"
    override fun toString(): String {
        return "Callable"
    }
}

fun test2() {
    assert(Callable()() == "World")
}

fun test3() {
    assert(Callable()(listOf(1,2,3)) == "World")
}

fun test4() {
    assert(Callable()(1, listOf(1,2,3)) == "World")
}

class NamedLambda<R>(val name: String, val function: () -> R) {
    operator fun invoke(): R = function()
    override fun toString(): String = "Lambda: $name"
}

fun test5() {
    assert(listOf(NamedLambda("Hello") { "Hello" }, NamedLambda("World") { "World" })[1]() == "Hello")
}

fun foo(x: Int) = x % 2 != 0

fun test6() {
    assert((::foo)(4))
}

