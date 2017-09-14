package local.declarations

import bar.*

/*p:local.declarations*/fun f(p: /*p:local.declarations p:kotlin*/Any) /*p:kotlin(Int)*/{
    /*p:kotlin(Any) p:kotlin(String)*/p.toString()

    val a = /*p:kotlin(Int)*/1
    val b = /*p:kotlin(Int)*/a
    fun localFun() = /*p:kotlin(Int)*/b
    fun /*p:local.declarations p:kotlin*/Int.localExtFun() = /*p:kotlin(Int)*/localFun()

    abstract class LocalI {
        abstract var a: /*p:local.declarations p:kotlin*/Int
        abstract fun foo()
    }

    class LocalC : LocalI() {
        override var a = /*p:kotlin(Int)*/1

        override fun foo() {}

        var b = /*p:kotlin(String)*/"bbb"

        fun bar() = /*p:kotlin(Int)*/b
    }

    val o = object {
        val a = /*p:kotlin(String)*/"aaa"
        fun foo(): LocalI = /*p:kotlin(Nothing)*/null as LocalI
    }

    /*p:kotlin(Int)*/localFun()
    /*p:kotlin(Int)*/1./*c:kotlin.Int(getLocalExtFun) c:kotlin.Int(getLOCALExtFun)*/localExtFun()

    val c = LocalC()
    /*p:kotlin(Int)*/c.a
    /*p:kotlin(String)*/c.b
    c.foo()
    /*p:kotlin(Int)*/c.bar()

    val i: LocalI = c
    /*p:kotlin(Int)*/i.a
    i.foo()

    /*p:kotlin(String)*/o.a
    val ii = o.foo()
    /*p:kotlin(Int)*/ii.a
}
