package foo.bar

/*p:foo.bar*/fun testOther(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) {
    /*c:foo.bar.A(set) p:foo.bar(set)*/a[1] = /*c:foo.bar.A(get)*/a[2]

    b /*c:foo.bar.A(contains)*/in a
    "s" /*c:foo.bar.A(contains) p:foo.bar(contains)*/!in a

    /*c:foo.bar.A(invoke)*/a()
    /*c:foo.bar.A(invoke) p:foo.bar(invoke)*/a(1)

    val (/*c:foo.bar.A(component1)*/h, /*c:foo.bar.A(component2) p:foo.bar(component2)*/t) = a;

    for ((/*c:foo.bar.A(component1)*/f, /*c:foo.bar.A(component2) p:foo.bar(component2)*/s) in /*c:foo.bar.A(iterator) c:foo.bar.A(hasNext) p:foo.bar(hasNext) c:foo.bar.A(next)*/a);
    for ((/*c:foo.bar.A(component1)*/f, /*c:foo.bar.A(component2) p:foo.bar(component2)*/s) in /*c:foo.bar.A(iterator) p:foo.bar(iterator) c:foo.bar.A(hasNext) p:foo.bar(hasNext) c:foo.bar.A(next)*/na);
}
