open class C: A() {
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

    C().foo()
    C().bar(true)
    C().baz()
}