package b

import a.Test

open class Foo {
    open class Bar {

    }
}

fun Test.test() {
    val aFoo: a.Foo = a.Foo()
    val bFoo: Foo = Foo()
    val cFoo: c.Foo = c.Foo()
    val aBar: a.Foo.Bar = a.Foo.Bar()
    val bBar: Foo.Bar = Foo.Bar()
    val cBar: c.Foo.Bar = c.Foo.Bar()

    fun foo(u: Int) {
        class T(val t: Int)
        object O {
            val t: Int = 1
        }

        val v = T(u).t + O.t
        println(v)
    }

    foo(1)
}