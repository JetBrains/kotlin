open class A {
    open fun <caret>foo(n: Int, s: String) {

    }

    open fun bar(b: Boolean, n: Int, s: String) {
        foo(n, s)
    }

    open fun baz() {
        foo(1, "abc")
        bar(false, 1, "abc")
    }
}

class B : A() {
    override fun foo(n: Int, s: String) {

    }

    override fun bar(b: Boolean, n: Int, s: String) {
        foo(n, s)
    }

    override fun baz() {
        foo(1, "abc")
        bar(false, 1, "abc")
    }
}

fun test(n: Int, s: String) {
    A().foo(n, s)
    A().bar(true, n, s)
    A().baz()

    B().foo(n, s)
    B().bar(true, n, s)
    B().baz()

    J().foo(n, s)
    J().bar(true, n, s)
    J().baz()
}