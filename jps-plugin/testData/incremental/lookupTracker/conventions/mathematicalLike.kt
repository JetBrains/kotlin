package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = a

    d/*c:foo.bar.A(inc)*/++
    /*c:foo.bar.A(inc)*/++d
    d/*c:foo.bar.A(dec) p:foo.bar(dec)*/--
    /*c:foo.bar.A(dec) p:foo.bar(dec)*/--d

    a /*c:foo.bar.A(plus)*/+ b
    a /*c:foo.bar.A(minus) p:foo.bar(minus)*/- b
    /*c:foo.bar.A(not) p:foo.bar(not)*/!a

    // for val
    a /*c:foo.bar.A(timesAssign)*/*= b
    a /*c:foo.bar.A(divAssign) p:foo.bar(divAssign)*//= b

    // for var
    d /*c:foo.bar.A(plusAssign) p:foo.bar(plusAssign) c:foo.bar.A(plus)*/+= b
    d /*c:foo.bar.A(minusAssign) p:foo.bar(minusAssign) c:foo.bar.A(minus) p:foo.bar(minus)*/-= b
    d /*c:foo.bar.A(timesAssign) c:foo.bar.A(times) p:foo.bar(times)*/*= b
    d /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) c:foo.bar.A(div) p:foo.bar(div)*//= b
}
