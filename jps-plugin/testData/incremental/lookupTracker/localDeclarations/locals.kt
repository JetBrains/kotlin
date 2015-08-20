package local.declarations

import bar.*

/*p:local.declarations*/fun f(p: /*p:local.declarations*/Any) {
    p.toString()

    val a = 1
    val b = a
    fun localFun() = b
    fun /*p:local.declarations*/Int.localExtFun() = /*p:local.declarations p:bar*/localFun()

    interface LocalI {
        var a: /*p:local.declarations*/Int
        fun foo()
    }

    class LocalC : LocalI {
        override var a = 1

        override fun foo() {}

        var b = "bbb"

        fun bar() = b
    }

    val o = object {
        val a = "aaa"
        fun foo(): LocalI = null as LocalI
    }

    /*p:local.declarations p:bar*/localFun()
    1./*p:local.declarations p:bar*/localExtFun()

    val c = /*p:local.declarations p:bar*/LocalC()
    c.a
    c.b
    c.foo()
    c.bar()

    val i: LocalI = c
    i.a
    i.foo()

    o.a
    val ii = o.foo()
    ii.a
}
