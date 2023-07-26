package foo.bar

/*p:foo.bar*/fun testComparisons(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) {
    a == c
    a != c
    na == a
    na == null

    /*p:A(compareTo) p:foo.bar.A(compareTo) p:kotlin(Int)*/a > b
    /*p:A(compareTo) p:foo.bar.A(compareTo) p:kotlin(Int)*/a < b
    /*p:A(compareTo) p:foo.bar.A(compareTo) p:kotlin(Int)*/a >= b
    /*p:A(compareTo) p:foo.bar.A(compareTo) p:kotlin(Int)*/a <= b

    /*p:A(compareTo) p:foo.bar(compareTo) p:foo.bar.A(compareTo) p:kotlin(Int)*/a > c
    /*p:A(compareTo) p:foo.bar(compareTo) p:foo.bar.A(compareTo) p:kotlin(Int)*/a < c
    /*p:A(compareTo) p:foo.bar(compareTo) p:foo.bar.A(compareTo) p:kotlin(Int)*/a >= c
    /*p:A(compareTo) p:foo.bar(compareTo) p:foo.bar.A(compareTo) p:kotlin(Int)*/a <= c
}
