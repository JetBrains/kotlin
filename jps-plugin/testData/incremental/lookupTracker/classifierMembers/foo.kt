package foo

import bar.*

/*p:foo*/class A {
    val a = 1
    var b = ""

    val c: /*c:foo.A c:foo.A.Companion p:foo*/String
        get() = /*c:foo.A*/b

    var d: /*c:foo.A c:foo.A.Companion p:foo*/String = "ddd"
        get() = field
        set(v) { field = v }

    fun foo() {
        /*c:foo.A*/a
        /*c:foo.A*/foo()
        this./*c:foo.A*/a
        this./*c:foo.A*/foo()
        /*c:foo.A c:foo.A.Companion*/baz()
        /*c:foo.A*/Companion./*c:foo.A.Companion*/a
        /*c:foo.A*/O./*c:foo.A.O*/v = "OK"
    }

    class B {
        val a = 1

        companion object CO {
            fun bar(a: /*c:foo.A.B.CO c:foo.A.B c:foo.A c:foo.A.Companion p:foo*/Int) {}
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
    var a: /*c:foo.I p:foo*/Int
    fun foo()

    class NI
}

/*p:foo*/object Obj : /*p:foo*/I {
    override var a = 1
    override fun foo() {}
    val b = 1
    fun bar(): /*c:foo.Obj p:foo*/I = null as /*c:foo.Obj p:foo*/I
}

/*p:foo*/enum class E {
    X,
    Y;

    val a = 1
    fun foo() {
        /*c:foo.E*/a
        /*c:foo.E*/Y./*c:foo.E*/a
        /*c:foo.E*/foo()
        /*c:foo.E*/X./*c:foo.E*/foo()
    }
}
