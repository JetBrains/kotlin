internal interface I {
    fun foo(i: Int, c: Char, s: String?)
    fun bar()
}

internal open class A : I {
    override fun foo(i: Int, c: Char, s: String?) {
        println("foo$i$c$s")
    }

    fun foo(i: Int, c: Char) {
        foo(i, c, "")
    }

    fun foo(i: Int) {
        foo(i, 'a', "")
    }

    override fun bar() {
        bar(1)
    }

    fun bar(i: Int) {}

    open fun x() {
        x(1)
    }

    fun x(i: Int) {}

    fun y() {
        y(1)
    }

    open fun y(i: Int) {}
}

internal class B : A() {
    override fun x() {
        super.x()
    }

    override fun y(i: Int) {
        super.y(i)
    }
}