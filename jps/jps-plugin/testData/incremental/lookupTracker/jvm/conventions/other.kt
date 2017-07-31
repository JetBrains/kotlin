package foo.bar

/*p:foo.bar*/fun testOther(a: /*p:foo.bar*/A, b: /*p:foo.bar p:kotlin*/Int, c: /*p:foo.bar p:kotlin*/Any, na: /*p:foo.bar*/A?) {
    /*p:foo.bar(A) c:foo.bar.A(set) c:foo.bar.A(getSet) c:foo.bar.A(getSET) p:foo.bar(set)*/a[1] = /*p:foo.bar(A) c:foo.bar.A(get) p:kotlin(Int)*/a[2]

    /*p:kotlin(Int) p:kotlin(Boolean)*/b /*c:foo.bar.A(contains)*/in /*p:foo.bar(A)*/a
    /*p:kotlin(String) p:kotlin(Boolean)*/"s" /*c:foo.bar.A(contains) c:foo.bar.A(getContains) c:foo.bar.A(getCONTAINS) p:foo.bar(contains)*/!in /*p:foo.bar(A)*/a

    /*c:foo.bar.A(invoke)*/a()
    /*c:foo.bar.A(invoke) p:foo.bar p:foo.bar(invoke)*/a(1)

    val (/*c:foo.bar.A(component1)*/h, /*c:foo.bar.A(component2) c:foo.bar.A(getComponent2) p:foo.bar(component2)*/t) = /*p:foo.bar(A)*/a;

    for ((/*c:foo.bar.A(component1)*/f, /*c:foo.bar.A(component2) c:foo.bar.A(getComponent2) p:foo.bar(component2)*/s) in /*p:foo.bar(A) c:foo.bar.A(iterator) c:foo.bar.A(hasNext) c:foo.bar.A(getHasNext) c:foo.bar.A(getHASNext) p:foo.bar(hasNext) c:foo.bar.A(next)*/a);
    for ((/*c:foo.bar.A(component1)*/f, /*c:foo.bar.A(component2) c:foo.bar.A(getComponent2) p:foo.bar(component2)*/s) in /*p:foo.bar(A) c:foo.bar.A(iterator) c:foo.bar.A(getIterator) c:foo.bar.A(getITERATOR) p:foo.bar(iterator) c:foo.bar.A(hasNext) c:foo.bar.A(getHasNext) c:foo.bar.A(getHASNext) p:foo.bar(hasNext) c:foo.bar.A(next)*/na);
}
