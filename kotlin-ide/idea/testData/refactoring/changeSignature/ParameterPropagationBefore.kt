open class A {
    open fun <caret>foo() {

    }

    open fun bar(b: Boolean) {
        foo()
    }

    open fun baz() {
        foo()
        bar(false)
    }
}

class B : A() {
    override fun foo() {

    }

    override fun bar(b: Boolean) {
        foo()
    }

    override fun baz() {
        foo()
        bar(false)
    }
}

fun test() {
    A().foo()
    A().bar(true)
    A().baz()

    B().foo()
    B().bar(true)
    B().baz()

    J().foo()
    J().bar(true)
    J().baz()
}