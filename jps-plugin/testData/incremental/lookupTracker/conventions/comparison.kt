package foo.bar

/*p:foo.bar*/fun testComparisons(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) {
    a /*c:foo.bar.A(equals)*/== c
    a /*c:foo.bar.A(equals)*/!= c
    na /*c:foo.bar.A(equals)*/== a
    na /*c:foo.bar.A(equals)*/== null

    a /*c:foo.bar.A(compareTo)*/> b
    a /*c:foo.bar.A(compareTo)*/< b
    a /*c:foo.bar.A(compareTo)*/>= b
    a /*c:foo.bar.A(compareTo)*/<= b

    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo)*/> c
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo)*/< c
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo)*/>= c
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo)*/<= c
}
