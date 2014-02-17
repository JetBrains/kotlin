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

fun Any.testInTopLevel() {
    assert(bar(), "Any.bar()", "bar()")
    assert(this.bar(), "Any.bar()", "this.bar()")
    assert(boo(this), "boo(Any)", "boo(this)")

    if (this is A) {
        assert(foo(47), "A.foo(47)", "foo(47)")
        assert(bar(), "A.bar()", "bar()")

        assert(this.foo(47), "A.foo(47)", "this.foo(47)")
        assert(this.bar(), "A.bar()", "this.bar()")

        assert(boo(this), "boo(A)", "boo(this: A)")
    }
}

class B {
    fun Any.test() {
        assert(bar(), "Any.bar()", "bar()")
        assert(this.bar(), "Any.bar()", "this.bar()")
        assert(boo(this), "boo(Any)", "boo(this)")

        if (this is A) {
            assert(foo(47), "A.foo(47)", "foo(47)")
            assert(bar(), "A.bar()", "bar()")

            assert(this.foo(47), "A.foo(47)", "this.foo(47)")
            assert(this.bar(), "A.bar()", "this.bar()")

            assert(boo(this), "boo(A)", "boo(this: A)")
        }
    }

    fun testInClass() {
        A().test()
    }
}

fun box(): String {
    A().testInTopLevel()
    B().testInClass()
    return "OK"
}
