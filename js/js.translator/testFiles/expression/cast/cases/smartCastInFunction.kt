package foo

class A {
    fun foo(a: Int) = "A.foo($a)"
}

fun Any.bar() = "Any.bar()"
fun A.bar() = "A.bar()"

fun boo(a: Any) = "boo(Any)"
fun boo(a: A) = "boo(A)"

fun assert<T>(expected: T, actual: T, caseName: String) {
    if (expected != actual) throw Exception("Filed on $caseName, expected: $expected, actual: $actual")
}

fun testInTopLevel(a: Any) {
    assert(a.bar(), "Any.bar()", "bar()")
    assert(a.bar(), "Any.bar()", "this.bar()")
    assert(boo(a), "boo(Any)", "boo(this)")

    if (a is A) {
        assert(a.foo(47), "A.foo(47)", "a.foo(47)")
        assert(a.bar(), "A.bar()", "a.bar()")
        assert(boo(a), "boo(A)", "boo(a: A)")
    }
}

class B {
    fun testInClass(a: Any) {
        assert(a.bar(), "Any.bar()", "bar()")
        assert(a.bar(), "Any.bar()", "this.bar()")
        assert(boo(a), "boo(Any)", "boo(this)")

        if (a is A) {
            assert(a.foo(47), "A.foo(47)", "a.foo(47)")
            assert(a.bar(), "A.bar()", "a.bar()")
            assert(boo(a), "boo(A)", "boo(a: A)")
        }
    }

}

fun box(): String {
    testInTopLevel(A())
    B().testInClass(A())

    return "OK"
}
