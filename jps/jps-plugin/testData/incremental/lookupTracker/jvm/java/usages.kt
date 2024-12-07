package foo

/*p:bar(C)*/import bar.C
import baz.*

/*p:foo*/fun usages() {
    val c = /*p:bar*/C()

    /*p:bar(C) p:bar.C(field)*/c.field
    /*p:bar(C) p:bar.C(field)*/c.field = 2
    /*p:bar(C) p:bar.C(func)*/c.func()
    /*p:bar(C) p:bar.C(B)*/c.B()

    /*p:bar.C(sfield) p:baz p:foo*/C.sfield
    /*p:bar.C(sfield) p:baz p:foo*/C.sfield = "new"
    /*p:bar.C(sfunc) p:baz p:foo*/C.sfunc()
    /*p:bar.C(S) p:baz p:foo*/C.S()

    // inherited from I
    /*p:bar(C) p:bar.C(ifunc)*/c.ifunc()
    /*p:bar.C(isfield) p:baz p:foo p:foo.I(isfield)*/C.isfield
    // expected error: Unresolved reference: IS
    /*p:bar.C(IS) p:baz p:baz(IS) p:foo p:foo(IS)*/C.IS()


    val i: /*p:baz p:foo*/I = /*p:bar(C)*/c
    /*p:foo(I) p:foo.I(ifunc)*/i.ifunc()

    /*p:baz p:foo p:foo.I(isfield)*/I.isfield
    /*p:baz p:foo p:foo.I(IS)*/I.IS()

    /*p:baz p:baz.E(F) p:foo*/E./*p:baz(E)*/F
    /*p:baz p:baz.E(F) p:baz.E(field) p:foo*/E./*p:baz(E)*/F.field
    /*p:baz p:baz.E(S) p:baz.E(func) p:foo*/E./*p:baz(E)*/S.func()
}

fun classifiers(
    c: /*p:bar p:baz p:foo*/C,
    b: /*p:bar.C(B) p:baz p:baz.C(B) p:foo p:foo.C(B)*/C.B,
    s: /*p:bar.C(S) p:baz p:baz.C(S) p:foo p:foo.C(S)*/C.S,
    cis: /*p:bar p:baz p:baz.C(IS) p:foo p:foo.C(IS)*/C.IS,
    i: /*p:baz p:foo*/I,
    iis: /*p:baz p:baz.I(IS) p:foo p:foo.I(IS)*/I.IS,
    e: /*p:baz p:foo*/E
) {}
