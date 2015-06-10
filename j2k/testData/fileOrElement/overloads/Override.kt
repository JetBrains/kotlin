interface I {
    public fun foo(i: Int, c: Char, s: String)
    public fun bar()
}

open class A : I {
    override fun foo(i: Int, c: Char, s: String) {
        println("foo$i$c$s")
    }

    public fun foo(i: Int, c: Char) {
        foo(i, c, "")
    }

    public fun foo(i: Int) {
        foo(i, 'a', "")
    }

    override fun bar() {
        bar(1)
    }

    public fun bar(i: Int) {
    }

    public open fun x() {
        x(1)
    }

    public fun x(i: Int) {
    }

    public fun y() {
        y(1)
    }

    public open fun y(i: Int) {
    }
}

class B : A() {
    override fun x() {
        super.x()
    }

    override fun y(i: Int) {
        super.y(i)
    }
}
