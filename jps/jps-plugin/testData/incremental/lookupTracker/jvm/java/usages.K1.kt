package foo

/*p:bar(C)*/import bar.C
import baz.*

/*p:foo*/fun usages() {
    val c = /*p:bar*/C()

    /*p:bar(C)*/c./*p:bar.C*/field
    /*p:bar(C)*/c./*p:bar.C*/field = 2
    /*p:bar(C)*/c./*p:bar.C*/func()
    /*p:bar(C) p:bar.C(B)*/c./*p:bar.C*/B()

    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*p:bar.C*/sfield
    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*p:bar.C*/sfield = "new"
    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*p:bar.C*/sfunc()
    /*p:bar p:bar.C(S) p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*p:bar.C*/S()

    // inherited from I
    /*p:bar(C)*/c./*p:bar.C*/ifunc()
    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*p:bar.C*/isfield
    // expected error: Unresolved reference: IS
    /*p:bar p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*p:bar.C*/IS()


    val i: /*p:foo*/I = /*p:bar(C)*/c
    /*p:foo(I)*/i./*p:foo.I*/ifunc()

    /*p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/I./*p:foo.I*/isfield
    /*p:baz p:foo p:foo.I(IS) p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/I./*p:foo.I*/IS()

    /*p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/E./*p:baz.E*/F
    /*p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/E./*p:baz.E*/F./*p:baz.E*/field
    /*p:baz p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/E./*p:baz.E*/S./*p:baz.E*/func()
}

/*p:foo*/fun classifiers(
    c: /*p:bar*/C,
    b: /*p:bar*/C./*p:bar.C*/B,
    s: /*p:bar*/C./*p:bar.C*/S,
    cis: /*p:bar*/C./*p:bar.C*/IS,
    i: /*p:foo*/I,
    iis: /*p:foo*/I./*p:foo.I*/IS,
    e: /*p:baz p:foo*/E
) {}
