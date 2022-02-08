package foo

/*p:bar(C)*/import bar.C
import baz.*

/*p:foo*/fun usages() {
    val c = /*p:bar*/C()

    /*p:bar(C) p:kotlin(Int)*/c./*c:bar.C*/field
    /*p:bar(C) p:kotlin(Int)*/c./*c:bar.C*/field = /*p:kotlin(Int)*/2
    /*p:bar(C)*/c./*c:bar.C*/func()
    /*c:bar.C(B) p:bar(C)*/c./*c:bar.C*/B()

    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin(String) p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*c:bar.C*/sfield
    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin(String) p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*c:bar.C*/sfield = /*p:kotlin(String)*/"new"
    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*c:bar.C*/sfunc()
    /*c:bar.C(S) p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*c:bar.C*/S()

    // inherited from I
    /*p:bar(C)*/c./*c:bar.C*/ifunc()
    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin(String) p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*c:bar.C*/isfield
    // expected error: Unresolved reference: IS
    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*c:bar.C*/IS()


    val i: /*p:foo*/I = /*p:bar(C)*/c
    /*p:foo(I)*/i./*c:foo.I*/ifunc()

    /*p:baz p:foo p:java.lang p:kotlin p:kotlin(String) p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/I./*c:foo.I*/isfield
    /*c:foo.I(IS) p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/I./*c:foo.I*/IS()

    /*p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/E./*c:baz.E*/F
    /*p:baz p:foo p:java.lang p:kotlin p:kotlin(Int) p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/E./*c:baz.E*/F./*c:baz.E*/field
    /*p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/E./*c:baz.E*/S./*c:baz.E*/func()
}

/*p:foo*/fun classifiers(
    c: /*p:bar*/C,
    b: /*p:bar*/C./*c:bar.C*/B,
    s: /*p:bar*/C./*c:bar.C*/S,
    cis: /*p:bar*/C./*c:bar.C*/IS,
    i: /*p:foo*/I,
    iis: /*p:foo*/I./*c:foo.I*/IS,
    e: /*p:baz p:foo*/E
) {}
