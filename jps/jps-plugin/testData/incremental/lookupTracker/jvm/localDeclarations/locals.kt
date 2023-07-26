package local.declarations

import bar.*

/*p:local.declarations*/fun f(p: /*p:bar p:local.declarations*/Any) {
    /*p:Any(toString) p:kotlin(String) p:kotlin.Any(toString)*/p.toString()

    val a = 1
    val b = a
    fun localFun() = b
    fun /*p:bar p:local.declarations*/Int.localExtFun() = /*p:kotlin(Int)*/localFun()

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

    /*p:kotlin(Int)*/localFun()
    /*p:IOT(localExtFun) p:kotlin(Int)*/1.localExtFun()

    val c = LocalC()
    /*p:kotlin(Int)*/c.a
    /*p:kotlin(String)*/c.b
    /*p:kotlin(Unit)*/c.foo()
    /*p:kotlin(Int)*/c.bar()

    val i: /*p:bar p:local.declarations*/LocalI = c
    /*p:kotlin(Int)*/i.a
    /*p:kotlin(Unit)*/i.foo()

    /*p:<anonymous>(a) p:kotlin(String)*/o.a
    val ii = /*p:<anonymous>(foo)*/o.foo()
    /*p:kotlin(Int)*/ii.a
}
