package foo

/*p:foo*/fun useAChild(a: /*p:foo*/AChild) {
    /*p:foo(AChild) p:foo.A(f) p:foo.AChild(f)*/a.f()
}