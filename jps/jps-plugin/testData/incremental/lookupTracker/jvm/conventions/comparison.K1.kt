package foo.bar

/*p:foo.bar*/fun testComparisons(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) {
    /*p:foo.bar(A)*/a /*p:foo.bar.A(equals)*/== c
    /*p:foo.bar(A)*/a /*p:foo.bar.A(equals)*/!= c
    /*p:foo.bar(A)*/na /*p:foo.bar.A(equals)*/== /*p:foo.bar(A)*/a
    /*p:foo.bar(A)*/na /*p:foo.bar.A(equals)*/== /*p:kotlin(Nothing)*/null

    /*p:foo.bar(A)*/a /*p:foo.bar.A(compareTo)*/> b
    /*p:foo.bar(A)*/a /*p:foo.bar.A(compareTo)*/< b
    /*p:foo.bar(A)*/a /*p:foo.bar.A(compareTo)*/>= b
    /*p:foo.bar(A)*/a /*p:foo.bar.A(compareTo)*/<= b

    /*p:foo.bar(A)*/a /*p:foo.bar(compareTo) p:foo.bar.A(compareTo) p:foo.bar.A(getCOMPARETo) p:foo.bar.A(getCompareTo)*/> c
    /*p:foo.bar(A)*/a /*p:foo.bar(compareTo) p:foo.bar.A(compareTo) p:foo.bar.A(getCOMPARETo) p:foo.bar.A(getCompareTo)*/< c
    /*p:foo.bar(A)*/a /*p:foo.bar(compareTo) p:foo.bar.A(compareTo) p:foo.bar.A(getCOMPARETo) p:foo.bar.A(getCompareTo)*/>= c
    /*p:foo.bar(A)*/a /*p:foo.bar(compareTo) p:foo.bar.A(compareTo) p:foo.bar.A(getCOMPARETo) p:foo.bar.A(getCompareTo)*/<= c
}
