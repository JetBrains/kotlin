package foo.bar

/*p:foo.bar*/fun testOther(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) {
    /*p:foo.bar(set) p:foo.bar.A(set) p:kotlin(Unit)*/a[1] = /*p:A(get) p:foo.bar.A(get) p:kotlin(Int)*/a[2]

    /*p:A(contains) p:foo.bar.A(contains) p:kotlin(Boolean)*/b in a
    /*p:A(contains) p:Boolean(not) p:foo.bar(contains) p:foo.bar.A(contains) p:kotlin(Boolean) p:kotlin.Boolean(not)*/"s" !in a

    /*p:A(invoke) p:foo.bar(A) p:foo.bar.A(invoke) p:kotlin(Unit)*/a()
    /*p:A(invoke) p:foo.bar p:foo.bar(A) p:foo.bar(invoke) p:foo.bar.A(invoke) p:kotlin(Unit)*/a(1)

    val (/*p:A(component1) p:foo.bar(A) p:foo.bar.A(component1)*/h, /*p:foo.bar(A) p:foo.bar(component2) p:foo.bar.A(component2)*/t) = a;

    for ((/*p:A(component1) p:foo.bar(A) p:foo.bar.A(component1)*/f, /*p:foo.bar(A) p:foo.bar(component2) p:foo.bar.A(component2)*/s) in /*p:A(iterator) p:A(next) p:foo.bar(A) p:foo.bar(hasNext) p:foo.bar.A(hasNext) p:foo.bar.A(iterator) p:foo.bar.A(next) p:kotlin(Boolean)*/a);
    for ((/*p:A(component1) p:foo.bar(A) p:foo.bar.A(component1)*/f, /*p:foo.bar(A) p:foo.bar(component2) p:foo.bar.A(component2)*/s) in /*p:A(iterator) p:A(next) p:foo.bar(A) p:foo.bar(hasNext) p:foo.bar(iterator) p:foo.bar.A(hasNext) p:foo.bar.A(next) p:foo.bar.A?(iterator) p:kotlin(Boolean)*/na);
}
