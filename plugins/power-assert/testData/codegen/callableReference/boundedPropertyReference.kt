// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT
fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
    "test5" to { test5() },
    "test6" to { test6() },
    "test7" to { test7("Some text") },
    "test8" to { test8("a", "b") },
    "test9" to { test9("Some text", "Another text") },
    "test10" to { test10("Some text") },
)

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

fun test1() {
    assert(A::foo == { true })
}

fun test2() {
    assert(A::foo.isOpen)
}

fun test3() {
    assert((A::foo)(A()))
}

fun test4() {
    assert(List<Int>::bar.name == "foo")
}

fun test5() {
    assert(List<Int>::bar.isConst)
}

fun test6() {
    assert((List<Int>::bar)(listOf(1,2,3)))
}

val String.foo: Boolean
    get() = false

fun test7(a: String) {
    assert((a::foo)())
}

fun test8(a: String, b: String) {
    assert(((a+b)::foo)())
}

fun test9(a: String, b: String) {
    assert(a::foo == b::foo)
}

fun test10(a: String) {
    assert(a::foo.isOpen)
}
