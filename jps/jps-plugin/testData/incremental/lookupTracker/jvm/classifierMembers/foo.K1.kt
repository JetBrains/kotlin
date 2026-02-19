package foo

/*p:foo*/class A {
    val a = 1
    var b = ""

    val c: /*p:foo p:foo.A p:foo.A.Companion*/String
        get() = /*p:foo.A*/b

    var d: /*p:foo p:foo.A p:foo.A.Companion*/String = "ddd"
        get() = field
        set(v) { field = v }

    fun foo() {
        /*p:foo.A*/a
        /*p:foo.A*/foo()
        /*p:foo(A)*/this./*p:foo.A*/a
        /*p:foo(A)*/this./*p:foo.A*/foo()
        /*p:foo p:foo.A p:foo.A(getBAZ) p:foo.A(getBaz) p:foo.A.Companion p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/baz()
        /*p:foo p:foo.A p:foo.A.Companion p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Companion./*p:foo.A.Companion*/a
        /*p:foo p:foo.A p:foo.A.Companion p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/O./*p:foo.A.O*/v = "OK"
    }

    class B {
        val a = 1

        companion object CO {
            fun bar(a: /*p:foo p:foo.A p:foo.A.B p:foo.A.B.CO p:foo.A.Companion*/Int) {}
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
    var a: /*p:foo p:foo.I*/Int
    fun foo()

    class NI
}

/*p:foo*/object Obj : /*p:foo*/I {
    override var a = 1
    override fun foo() {}
    val b = 1
    fun bar(): /*p:foo p:foo.Obj*/I = /*p:foo(I) p:kotlin(Nothing)*/null as /*p:foo p:foo.Obj*/I
}

/*p:foo*/enum class E {
    X,
    Y;

    val a = 1
    fun foo() {
        /*p:foo.E*/a
        /*p:foo p:foo.E p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Y./*p:foo.E*/a
        /*p:foo.E*/foo()
        /*p:foo p:foo.E p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/X./*p:foo.E*/foo()
    }
}
