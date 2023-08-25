package foo

/*p:bar(C)*/import bar.C
import baz.*

/*p:foo*/fun usages() {
    val c = /*p:bar*/C()

    /*p:C(field) p:bar.C(field) p:kotlin(Int)*/c.field
    /*p:C(field) p:bar.C(field) p:kotlin(Int)*/c.field = 2
    /*p:C(func) p:bar.C(func) p:kotlin(Unit)*/c.func()
    /*p:C.B(B) p:bar(B) p:bar.C(B)*/c.B()

    /*p:bar p:baz p:foo*/C.sfield
    /*p:bar p:baz p:foo*/C.sfield = "new"
    /*p:bar p:baz p:foo p:kotlin(Unit)*/C.sfunc()
    /*p:bar p:bar(S) p:baz p:foo*/C.S()

    // inherited from I
    /*p:C(ifunc) p:bar.C(ifunc) p:kotlin(Unit)*/c.ifunc()
    /*p:bar p:baz p:foo*/C.isfield
    // expected error: Unresolved reference: IS
    /*p:bar p:bar(IS) p:baz p:baz(IS) p:foo p:foo(IS)*/C.IS()


    val i: /*p:bar p:baz p:foo*/I = c
    /*p:I(ifunc) p:foo.I(ifunc) p:kotlin(Unit)*/i.ifunc()

    /*p:bar p:baz p:foo*/I.isfield
    /*p:bar p:baz p:foo p:foo(IS)*/I.IS()

    /*p:bar p:baz p:foo*/E.F
    /*p:E(field) p:bar p:baz p:baz.E(field) p:foo p:kotlin(Int)*/E.F.field
    /*p:E(func) p:bar p:baz p:baz.E(func) p:foo p:kotlin(Unit)*/E.S.func()
}

fun classifiers(
    c: /*p:bar p:baz p:foo*/C,
    b: /*p:bar p:baz p:foo*/C.B,
    s: /*p:bar p:baz p:foo*/C.S,
    cis: /*p:bar p:baz p:foo*/C.IS,
    i: /*p:bar p:baz p:foo*/I,
    iis: /*p:bar p:baz p:foo*/I.IS,
    e: /*p:bar p:baz p:foo*/E
) {}
