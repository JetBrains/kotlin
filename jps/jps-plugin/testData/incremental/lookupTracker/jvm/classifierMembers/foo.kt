package foo

import bar.*

/*p:foo*/class A {
    val a = 1
    var b = ""

    val c: /*p:bar p:foo*/String
        get() = /*p:foo.A p:kotlin(String)*/b

    var d: /*p:bar p:foo*/String = "ddd"
        get() = field
        set(v) { field = v }

    fun foo() {
        /*p:foo.A p:kotlin(Int)*/a
        /*p:foo.A p:kotlin(Unit)*/foo()
        /*p:foo.A(a) p:kotlin(Int)*/this.a
        /*p:foo.A(foo) p:kotlin(Unit)*/this.foo()
        /*p:bar p:foo p:foo.A p:foo.A.Companion p:kotlin(Unit)*/baz()
        /*p:bar p:foo p:foo.A p:foo.A(a) p:foo.A.Companion p:foo.A.Companion(a) p:kotlin(Int)*/Companion.a
        /*p:bar p:foo p:foo.A p:foo.A.Companion p:foo.A.O(v) p:kotlin(String)*/O.v = "OK"
    }

    class B {
        val a = 1

        companion object CO {
            fun bar(a: /*p:bar p:foo*/Int) {}
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
    var a: /*p:bar p:foo*/Int
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
        /*p:foo.E p:kotlin(Int)*/a
        /*p:bar p:foo p:foo.E p:foo.E(a) p:kotlin(Int) p:kotlin.Enum p:kotlin.Enum.Companion*/Y.a
        /*p:foo.E p:kotlin(Unit)*/foo()
        /*p:bar p:foo p:foo.E p:foo.E(foo) p:kotlin(Unit) p:kotlin.Enum p:kotlin.Enum.Companion*/X.foo()
    }
}
