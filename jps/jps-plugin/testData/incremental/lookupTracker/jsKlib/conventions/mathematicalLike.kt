package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = /*p:foo.bar(A)*/a

    /*p:foo.bar(A)*/d/*p:foo.bar(A) p:foo.bar.A(inc)*/++
    /*p:foo.bar(A) p:foo.bar.A(inc)*/++/*p:foo.bar(A)*/d
    /*p:foo.bar(A)*/d/*p:foo.bar(A) p:foo.bar(dec) p:foo.bar.A(dec)*/--
    /*p:foo.bar(A) p:foo.bar(dec) p:foo.bar.A(dec)*/--/*p:foo.bar(A)*/d

    /*p:foo.bar(A) p:foo.bar.A(plus)*/a + b
    /*p:foo.bar(A) p:foo.bar(minus) p:foo.bar.A(minus)*/a - b
    /*p:foo.bar(not) p:foo.bar.A(not)*/!/*p:foo.bar(A)*/a

    // for val
    /*p:foo.bar(A) p:foo.bar(times) p:foo.bar.A(times) p:foo.bar.A(timesAssign)*/a *= b
    /*p:foo.bar(A) p:foo.bar(div) p:foo.bar(divAssign) p:foo.bar.A(div) p:foo.bar.A(divAssign)*/a /= b

    // for var
    /*p:foo.bar(A) p:foo.bar(plusAssign) p:foo.bar.A(plus) p:foo.bar.A(plusAssign) p:kotlin.collections(plusAssign)*/d += b
    /*p:foo.bar(A) p:foo.bar(minus) p:foo.bar(minusAssign) p:foo.bar.A(minus) p:foo.bar.A(minusAssign) p:kotlin.collections(minusAssign)*/d -= b
    /*p:foo.bar(A) p:foo.bar(times) p:foo.bar.A(times) p:foo.bar.A(timesAssign)*/d *= b
    /*p:foo.bar(A) p:foo.bar(div) p:foo.bar(divAssign) p:foo.bar.A(div) p:foo.bar.A(divAssign)*/d /= b
}
