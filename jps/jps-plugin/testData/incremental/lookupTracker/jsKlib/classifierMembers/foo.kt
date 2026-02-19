package foo

import bar.*

/*p:foo*/class A {
    val a = 1
    var b = ""

    val c: /*p:bar p:foo p:foo.A*/String
        get() = /*p:foo.A*/b

    var d: /*p:bar p:foo p:foo.A*/String = "ddd"
        get() = field
        set(v) { field = v }

    fun foo() {
        /*p:foo.A*/a
        /*p:foo.A*/foo()
        /*p:foo.A(a)*/this.a
        /*p:foo.A(foo)*/this.foo()
        /*p:bar p:foo p:foo.A p:foo.A.Companion*/baz()
        /*p:bar p:foo p:foo.A p:foo.A(a) p:foo.A.Companion p:foo.A.Companion(a)*/Companion.a
        /*p:bar p:foo p:foo.A p:foo.A.Companion p:foo.A.O(v)*/O.v = "OK"
    }

    class B {
        val a = 1

        companion object CO {
            fun bar(a: /*p:bar p:foo p:foo.A p:foo.A.B*/Int) {}
        }
    }

    inner class C

    companion object {
        val a = 1
        fun baz() {}
    }

    object O {
        var v = "vvv"
    }
}

/*p:foo*/interface I {
    var a: /*p:bar p:foo p:foo.I*/Int
    fun foo()

    class NI
}

/*p:foo*/object Obj : /*p:bar p:foo*/I {
    override var a = 1
    override fun foo() {}
    val b = 1
    fun bar(): /*p:bar p:foo*/I = null as /*p:bar p:foo*/I
}

/*p:foo*/enum class E {
    X,
    Y;

    val a = 1
    fun foo() {
        /*p:foo.E*/a
        /*p:bar p:foo p:foo(E) p:foo.E p:foo.E(a) p:kotlin(Enum) p:kotlin.Enum p:kotlin.Enum(Companion) p:kotlin.Enum.Companion*/Y.a
        /*p:foo.E*/foo()
        /*p:bar p:foo p:foo(E) p:foo.E p:foo.E(foo) p:kotlin(Enum) p:kotlin.Enum p:kotlin.Enum(Companion) p:kotlin.Enum.Companion*/X.foo()
    }
}
