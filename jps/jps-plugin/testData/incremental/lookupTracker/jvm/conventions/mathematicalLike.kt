package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = a

    d/*p:A(inc) p:foo.bar(A) p:foo.bar.A(inc)*/++
    /*p:A(inc) p:foo.bar(A) p:foo.bar.A(inc)*/++d
    d/*p:foo.bar(A) p:foo.bar(dec) p:foo.bar.A(dec)*/--
    /*p:foo.bar(A) p:foo.bar(dec) p:foo.bar.A(dec)*/--d

    /*p:A(plus) p:foo.bar(A) p:foo.bar.A(plus)*/a + b
    /*p:foo.bar(A) p:foo.bar(minus) p:foo.bar.A(minus)*/a - b
    /*p:foo.bar(not) p:foo.bar.A(not) p:kotlin(Unit)*/!a

    // for val
    /*p:A(timesAssign) p:foo.bar(times) p:foo.bar.A(times) p:foo.bar.A(timesAssign) p:kotlin(Unit)*/a *= b
    /*p:foo.bar(div) p:foo.bar(divAssign) p:foo.bar.A(div) p:foo.bar.A(divAssign) p:kotlin(Unit)*/a /= b

    // for var
    /*p:A(plus) p:foo.bar(A) p:foo.bar(plusAssign) p:foo.bar.A(plus) p:foo.bar.A(plusAssign)*/d += b
    /*p:foo.bar(A) p:foo.bar(minus) p:foo.bar(minusAssign) p:foo.bar.A(minus) p:foo.bar.A(minusAssign)*/d -= b
    /*p:A(timesAssign) p:foo.bar(times) p:foo.bar.A(times) p:foo.bar.A(timesAssign) p:kotlin(Unit)*/d *= b
    /*p:foo.bar(div) p:foo.bar(divAssign) p:foo.bar.A(div) p:foo.bar.A(divAssign) p:kotlin(Unit)*/d /= b
}
