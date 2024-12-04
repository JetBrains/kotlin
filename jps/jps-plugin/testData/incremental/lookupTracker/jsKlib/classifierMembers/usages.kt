package foo

import bar.*

/*p:foo*/fun usages(i: /*p:bar p:foo*/I) {
    /*p:foo p:foo.A(a)*/A().a
    /*p:foo p:foo.A(b)*/A().b
    /*p:foo p:foo.A(c)*/A().c
    /*p:foo p:foo.A(d)*/A().d = "new value"
    /*p:foo p:foo.A(foo)*/A().foo()
    /*p:bar p:foo p:foo.A(B) p:foo.A(Companion) p:foo.A.B(a)*/A.B().a
    /*p:bar p:bar(bar) p:foo p:foo(bar) p:foo.A(B) p:foo.A(Companion) p:foo.A.B(CO) p:foo.A.B(bar) p:foo.A.B.CO(bar)*/A./*p:foo.A.B(CO)*/B.bar(1)
    /*p:bar p:bar(bar) p:foo p:foo(bar) p:foo.A(B) p:foo.A(Companion) p:foo.A.B(CO) p:foo.A.B.CO(bar)*/A./*p:foo.A.B(CO)*/B./*p:foo.A.B*/CO.bar(1)
    /*p:foo p:foo.A(Companion)*/A
    /*p:bar p:foo p:foo.A(Companion) p:foo.A(a) p:foo.A.Companion(a)*/A.a
    /*p:bar p:bar(baz) p:foo p:foo(baz) p:foo.A(Companion) p:foo.A(baz) p:foo.A.Companion(baz)*/A.baz()
    /*p:bar p:foo p:foo.A(Companion)*/A./*p:foo.A*/Companion
    /*p:bar p:bar(baz) p:foo p:foo(baz) p:foo.A(Companion) p:foo.A(baz) p:foo.A.Companion(baz)*/A./*p:foo.A*/Companion.baz()
    /*p:bar p:foo p:foo.A(Companion) p:foo.A(O)*/A./*p:foo.A*/O
    /*p:bar p:foo p:foo.A(Companion) p:foo.A(O) p:foo.A.O(v)*/A./*p:foo.A*/O.v = "OK"

    /*p:foo(I) p:foo.I(a)*/i.a = 2
    /*p:bar p:foo p:foo.Obj(a)*/Obj.a
    /*p:bar p:bar(foo) p:foo p:foo(foo) p:foo.Obj(foo)*/Obj.foo()
    var ii: /*p:bar p:foo*/I = /*p:foo*/Obj
    /*p:foo(I) p:foo.I(a)*/ii.a
    /*p:foo(I) p:foo.I(foo)*/ii.foo()
    /*p:bar p:foo p:foo.Obj(b)*/Obj.b
    val iii = /*p:bar p:bar(bar) p:foo p:foo(I) p:foo(bar) p:foo.Obj(bar)*/Obj.bar()
    /*p:foo(I) p:foo.I(foo)*/iii.foo()

    /*p:bar p:foo p:foo.E(X)*/E./*p:foo(E)*/X
    /*p:bar p:foo p:foo.E(X) p:foo.E(a)*/E./*p:foo(E)*/X.a
    /*p:bar p:foo p:foo.E(Y) p:foo.E(foo)*/E./*p:foo(E)*/Y.foo()
    /*p:bar p:foo p:foo.E(values) p:kotlin(Array)*/E.values()
    /*p:bar p:foo p:foo.E(valueOf)*/E.valueOf("")
}

/*p:foo*/fun classifiers(
        a: /*p:bar p:foo*/A,
        ab: /*p:bar p:bar.A(B) p:foo p:foo.A(B)*/A.B,
        ac: /*p:bar p:bar.A(C) p:foo p:foo.A(C)*/A.C,
        abCo: /*p:bar p:bar.A(B) p:bar.A.B(CO) p:foo p:foo.A(B) p:foo.A.B(CO)*/A.B.CO,
        aCompanion: /*p:bar p:bar.A(Companion) p:foo p:foo.A(Companion)*/A.Companion,
        aO: /*p:bar p:bar.A(O) p:foo p:foo.A(O)*/A.O,
        i: /*p:bar p:foo*/I,
        ni: /*p:bar p:bar.I(NI) p:foo p:foo.I(NI)*/I.NI,
        obj: /*p:bar p:foo*/Obj,
        e: /*p:bar p:foo*/E
) {}
