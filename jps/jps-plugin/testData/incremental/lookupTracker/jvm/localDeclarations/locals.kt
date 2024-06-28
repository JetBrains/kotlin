package local.declarations

import bar.*

/*p:local.declarations*/fun f(p: /*p:bar p:local.declarations*/Any) {
    /*p:kotlin.Any(toString)*/p.toString()

    val a = 1
    val b = a
    fun localFun() = b
    fun /*p:bar p:local.declarations*/Int.localExtFun() = localFun()

    abstract class LocalI {
        abstract var a: /*p:bar p:local.declarations*/Int
        abstract fun foo()
    }

    class LocalC : /*p:bar p:local.declarations*/LocalI() {
        override var a = 1

        override fun foo() {}

        var b = "bbb"

        fun bar() = b
    }

    val o = object {
        val a = "aaa"
        fun foo(): /*p:bar p:local.declarations*/LocalI = null as /*p:bar p:local.declarations*/LocalI
    }

    localFun()
    1.localExtFun()

    val c = LocalC()
    c.a
    c.b
    c.foo()
    c.bar()

    val i: /*p:bar p:local.declarations*/LocalI = c
    i.a
    i.foo()

    o.a
    val ii = o.foo()
    ii.a
}
