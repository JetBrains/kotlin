package foo

/*p:bar(C)*/import bar.C
import baz.*

/*p:foo*/fun usages() {
    val c = /*p:bar*/C()

    /*p:bar(C) p:kotlin(Int)*/c./*c:bar.C*/field
    /*p:bar(C) p:kotlin(Int)*/c./*c:bar.C*/field = /*p:kotlin(Int)*/2
    /*p:bar(C)*/c./*c:bar.C*/func()
    /*p:bar(C) c:bar.C(B)*/c./*c:bar.C*/B()

    /*p:bar p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:kotlin(String)*/C./*c:bar.C*/sfield
    /*p:bar p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:kotlin(String)*/C./*c:bar.C*/sfield = /*p:kotlin(String)*/"new"
    /*p:bar p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm*/C./*c:bar.C*/sfunc()
    /*p:bar p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm c:bar.C(S)*/C./*c:bar.C*/S()

    // inherited from I
    /*p:bar(C)*/c./*c:bar.C*/ifunc()
    /*p:bar p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:kotlin(String)*/C./*c:bar.C*/isfield
    // expected error: Unresolved reference: IS
    /*p:bar p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm*/C./*c:bar.C*/IS()


    val i: /*p:foo*/I = /*p:bar(C)*/c
    /*p:foo(I)*/i./*c:foo.I*/ifunc()

    /*p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:kotlin(String)*/I./*c:foo.I*/isfield
    /*p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm c:foo.I(IS)*/I./*c:foo.I*/IS()

    /*p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm*/E./*c:baz.E*/F
    /*p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:kotlin(Int)*/E./*c:baz.E*/F./*c:baz.E*/field
    /*p:foo p:baz p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm*/E./*c:baz.E*/S./*c:baz.E*/func()
}

/*p:foo*/fun classifiers(
    c: /*p:bar*/C,
    b: /*p:bar*/C./*c:bar.C*/B,
    s: /*p:bar*/C./*c:bar.C*/S,
    cis: /*p:bar*/C./*c:bar.C*/IS,
    i: /*p:foo*/I,
    iis: /*p:foo*/I./*c:foo.I*/IS,
    e: /*p:foo p:baz*/E
) {}
