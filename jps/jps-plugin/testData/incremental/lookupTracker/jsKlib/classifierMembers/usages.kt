package foo

import bar.*

/*p:foo*/fun usages(i: /*p:bar p:foo*/I) {
    /*p:A(a) p:foo p:foo.A(a) p:kotlin(Int)*/A().a
    /*p:A(b) p:foo p:foo.A(b) p:kotlin(String)*/A().b
    /*p:A(c) p:foo p:foo.A(c) p:kotlin(String)*/A().c
    /*p:A(d) p:foo p:foo.A(d) p:kotlin(String)*/A().d = "new value"
    /*p:A(foo) p:foo p:foo.A(foo) p:kotlin(Unit)*/A().foo()
    /*p:A.B(a) p:foo p:foo(B) p:foo.A.B(a) p:kotlin(Int)*/A.B().a
    /*p:A.B.CO(bar) p:bar(bar) p:foo p:foo(bar) p:foo.A.B.CO(bar) p:kotlin(Unit)*/A.B.bar(1)
    /*p:A.B.CO(bar) p:bar(bar) p:foo p:foo(bar) p:foo.A.B.CO(bar) p:kotlin(Unit)*/A.B.CO.bar(1)
    /*p:foo*/A
    /*p:A.Companion(a) p:foo p:foo.A(a) p:foo.A.Companion(a) p:kotlin(Int)*/A.a
    /*p:A.Companion(baz) p:bar(baz) p:foo p:foo(baz) p:foo.A(baz) p:foo.A.Companion(baz) p:kotlin(Unit)*/A.baz()
    /*p:foo*/A.Companion
    /*p:A.Companion(baz) p:bar(baz) p:foo p:foo(baz) p:foo.A(baz) p:foo.A.Companion(baz) p:kotlin(Unit)*/A.Companion.baz()
    /*p:foo*/A.O
    /*p:A.O(v) p:foo p:foo.A.O(v) p:kotlin(String)*/A.O.v = "OK"

    /*p:I(a) p:foo.I(a) p:kotlin(Int)*/i.a = 2
    /*p:Obj(a) p:foo p:foo.Obj(a) p:kotlin(Int)*/Obj.a
    /*p:Obj(foo) p:bar(foo) p:foo p:foo(foo) p:foo.Obj(foo) p:kotlin(Unit)*/Obj.foo()
    var ii: /*p:bar p:foo*/I = /*p:foo*/Obj
    /*p:I(a) p:foo.I(a) p:kotlin(Int)*/ii.a
    /*p:I(foo) p:foo.I(foo) p:kotlin(Unit)*/ii.foo()
    /*p:Obj(b) p:foo p:foo.Obj(b) p:kotlin(Int)*/Obj.b
    val iii = /*p:Obj(bar) p:bar(bar) p:foo p:foo(I) p:foo(bar) p:foo.Obj(bar)*/Obj.bar()
    /*p:I(foo) p:foo.I(foo) p:kotlin(Unit)*/iii.foo()

    /*p:bar p:foo*/E.X
    /*p:E(a) p:bar p:foo p:foo.E(a) p:kotlin(Int)*/E.X.a
    /*p:E(foo) p:bar p:foo p:foo.E(foo) p:kotlin(Unit)*/E.Y.foo()
    /*p:bar p:foo p:kotlin(Array)*/E.values()
    /*p:bar p:foo*/E.valueOf("")
}

/*p:foo*/fun classifiers(
        a: /*p:bar p:foo*/A,
        ab: /*p:bar p:foo*/A.B,
        ac: /*p:bar p:foo*/A.C,
        abCo: /*p:bar p:foo*/A.B.CO,
        aCompanion: /*p:bar p:foo*/A.Companion,
        aO: /*p:bar p:foo*/A.O,
        i: /*p:bar p:foo*/I,
        ni: /*p:bar p:foo*/I.NI,
        obj: /*p:bar p:foo*/Obj,
        e: /*p:bar p:foo*/E
) {}
