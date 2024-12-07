package foo.bar

/*p:foo.bar*/fun testOther(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) {
    /*p:foo.bar(A) p:foo.bar(set) p:foo.bar.A(set)*/a[1] = /*p:foo.bar(A) p:foo.bar.A(get)*/a[2]

    /*p:foo.bar.A(contains)*/b in /*p:foo.bar(A)*/a
    /*p:foo.bar(contains) p:foo.bar.A(contains) p:kotlin.Boolean(not)*/"s" !in /*p:foo.bar(A)*/a

    /*p:foo.bar(A) p:foo.bar.A(invoke)*/a()
    /*p:foo.bar p:foo.bar(A) p:foo.bar(invoke) p:foo.bar.A(invoke)*/a(1)

    val (/*p:foo.bar(A) p:foo.bar.A(component1)*/h, /*p:foo.bar(A) p:foo.bar(component2) p:foo.bar.A(component2)*/t) = /*p:foo.bar(A)*/a;

    for ((/*p:foo.bar(A) p:foo.bar.A(component1)*/f, /*p:foo.bar(A) p:foo.bar(component2) p:foo.bar.A(component2)*/s) in /*p:foo.bar(A) p:foo.bar(hasNext) p:foo.bar.A(hasNext) p:foo.bar.A(iterator) p:foo.bar.A(next)*/a);
    for ((/*p:foo.bar(A) p:foo.bar.A(component1)*/f, /*p:foo.bar(A) p:foo.bar(component2) p:foo.bar.A(component2)*/s) in /*p:foo.bar(A) p:foo.bar(hasNext) p:foo.bar(iterator) p:foo.bar.A(hasNext) p:foo.bar.A(iterator) p:foo.bar.A(next)*/na);
}
