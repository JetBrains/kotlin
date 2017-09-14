package foo

import bar.*

/*p:foo*/class A {
    val a = /*p:kotlin(Int)*/1
    var b = /*p:kotlin(String)*/""

    val c: /*c:foo.A c:foo.A.Companion p:foo p:kotlin*/String
        get() = /*c:foo.A p:kotlin(String)*/b

    var d: /*c:foo.A c:foo.A.Companion p:foo p:kotlin*/String = /*p:kotlin(String)*/"ddd"
        get() = /*p:kotlin(String)*/field
        set(v) { /*p:kotlin(String)*/field = /*p:kotlin(String)*/v }

    fun foo() {
        /*c:foo.A p:kotlin(Int)*/a
        /*c:foo.A*/foo()
        /*p:foo(A) p:kotlin(Int)*/this./*c:foo.A*/a
        /*p:foo(A)*/this./*c:foo.A*/foo()
        /*c:foo.A c:foo.A(getBaz) c:foo.A(getBAZ) c:foo.A.Companion p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm*/baz()
        /*c:foo.A c:foo.A.Companion p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:kotlin(Int)*/Companion./*c:foo.A.Companion*/a
        /*c:foo.A c:foo.A.Companion p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:kotlin(String)*/O./*c:foo.A.O*/v = /*p:kotlin(String)*/"OK"
    }

    class B {
        val a = /*p:kotlin(Int)*/1

        companion object CO {
            fun bar(a: /*c:foo.A.B.CO c:foo.A.B c:foo.A c:foo.A.Companion p:foo p:kotlin*/Int) {}
        }
    }

    inner class C

    companion object {
        val a = /*p:kotlin(Int)*/1
        fun baz() {}
    }

    object O {
        var v = /*p:kotlin(String)*/"vvv"
    }
}

/*p:foo*/interface I {
    var a: /*c:foo.I p:foo p:kotlin*/Int
    fun foo()

    class NI
}

/*p:foo*/object Obj : /*p:foo*/I {
    override var a = /*p:kotlin(Int)*/1
    override fun foo() {}
    val b = /*p:kotlin(Int)*/1
    fun bar(): /*c:foo.Obj p:foo*/I = /*p:kotlin(Nothing) p:foo(I)*/null as /*c:foo.Obj p:foo*/I
}

/*p:foo*/enum class E {
    X,
    Y;

    val a = /*p:kotlin(Int)*/1
    fun foo() {
        /*c:foo.E p:kotlin(Int)*/a
        /*c:foo.E p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:kotlin(Int)*/Y./*c:foo.E*/a
        /*c:foo.E*/foo()
        /*c:foo.E p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm*/X./*c:foo.E*/foo()
    }
}
