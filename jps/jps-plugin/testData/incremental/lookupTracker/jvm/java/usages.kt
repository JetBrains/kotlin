package foo

/*p:bar(C)*/import bar.C
import baz.*

/*p:foo*/fun usages() {
    val c = /*p:bar*/C()

    /*p:bar.C(field)*/c.field
    /*p:bar.C(field)*/c.field = 2
    /*p:bar.C(func)*/c.func()
    /*p:bar(B) p:bar.C(B) p:bar.C.B(B)*/c.B()

    /*p:bar p:bar.C(sfield) p:baz p:foo*/C.sfield
    /*p:bar p:bar.C(sfield) p:baz p:foo*/C.sfield = "new"
    /*p:bar p:bar.C(sfunc) p:baz p:foo*/C.sfunc()
    /*p:bar p:bar(S) p:bar.C(S) p:baz p:foo*/C.S()

    // inherited from I
    /*p:bar.C(ifunc)*/c.ifunc()
    /*p:bar p:bar.C(isfield) p:baz p:foo*/C.isfield
    // expected error: Unresolved reference: IS
    /*p:bar p:bar(IS) p:bar.C(IS) p:baz p:baz(IS) p:foo p:foo(IS)*/C.IS()


    val i: /*p:bar p:baz p:foo*/I = c
    /*p:foo.I(ifunc)*/i.ifunc()

    /*p:bar p:baz p:foo p:foo.I(isfield)*/I.isfield
    /*p:bar p:baz p:foo p:foo(IS) p:foo.I(IS)*/I.IS()

    /*p:bar p:baz p:baz.E(F) p:foo*/E.F
    /*p:bar p:baz p:baz.E(F) p:baz.E(field) p:foo*/E.F.field
    /*p:bar p:baz p:baz.E(S) p:baz.E(func) p:foo*/E.S.func()
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
