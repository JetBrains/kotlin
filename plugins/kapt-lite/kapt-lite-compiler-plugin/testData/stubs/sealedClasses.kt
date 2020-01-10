package sealedClasses

sealed class Foo(val x: Int) {
    abstract fun foo(q: Int)

    class Bar(val a: String, x: Int) : Foo(x) {
        override fun foo(q: Int) {}
        fun bar(z: String) {}
    }

    object Baz : Foo(1) {
        override fun foo(q: Int) {}
    }
}

sealed class Foo2(val x: Int) {
    abstract fun foo(q: Int)
}

class Bar2(val a: String, x: Int) : Foo2(x) {
    override fun foo(q: Int) {}
    fun bar(z: String) {}
}

object Baz2 : Foo2(1) {
    override fun foo(q: Int) {}
}