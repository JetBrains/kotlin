interface Z {
    open fun foo(a: Int, b: Int)
}

open class A {
    open fun foo(<caret>a: Int, b: Int) {

    }
}

open class B: A(), Z {
    override fun foo(a: Int, b: Int) {

    }
}

class C: A() {
    override fun foo(a: Int, b: Int) {

    }
}

class D: B(), Z {
    override fun foo(a: Int, b: Int) {
        println(a)
    }
}

class U {
    fun bar(a: A) {
        a.foo(1, 2)
    }

    fun bar(b: B) {
        b.foo(3, 4)
    }

    fun bar(c: C) {
        c.foo(5, 6)
    }

    fun bar(d: D) {
        d.foo(7, 8)
    }

    fun bar(z: Z) {
        z.foo(9, 10)
    }
}