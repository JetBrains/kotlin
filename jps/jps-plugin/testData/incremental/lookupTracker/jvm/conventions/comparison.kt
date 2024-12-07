package foo.bar

/*p:foo.bar*/fun testComparisons(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) {
    /*p:foo.bar(A)*/a == c
    /*p:foo.bar(A)*/a != c
    /*p:foo.bar(A)*/na == /*p:foo.bar(A)*/a
    /*p:foo.bar(A)*/na == null

    /*p:foo.bar(A) p:foo.bar.A(compareTo)*/a > b
    /*p:foo.bar(A) p:foo.bar.A(compareTo)*/a < b
    /*p:foo.bar(A) p:foo.bar.A(compareTo)*/a >= b
    /*p:foo.bar(A) p:foo.bar.A(compareTo)*/a <= b

    /*p:foo.bar(A) p:foo.bar(compareTo) p:foo.bar.A(compareTo)*/a > c
    /*p:foo.bar(A) p:foo.bar(compareTo) p:foo.bar.A(compareTo)*/a < c
    /*p:foo.bar(A) p:foo.bar(compareTo) p:foo.bar.A(compareTo)*/a >= c
    /*p:foo.bar(A) p:foo.bar(compareTo) p:foo.bar.A(compareTo)*/a <= c
}
