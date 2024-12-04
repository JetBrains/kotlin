package local.declarations

import bar.*

/*p:local.declarations*/fun f(p: /*p:local.declarations*/Any) {
    p.toString()

    val a = 1
    val b = a
    fun localFun() = b
    fun /*p:local.declarations*/Int.localExtFun() = localFun()

    abstract class LocalI {
        abstract var a: /*p:local.declarations*/Int
        abstract fun foo()
    }

    class LocalC : LocalI() {
        override var a = 1

        override fun foo() {}

        var b = "bbb"

        fun bar() = b
    }

    val o = object {
        val a = "aaa"
        fun foo(): LocalI = /*p:kotlin(Nothing)*/null as LocalI
    }

    localFun()
    1./*p:kotlin.Int(getLOCALExtFun) p:kotlin.Int(getLocalExtFun)*/localExtFun()

    val c = LocalC()
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
