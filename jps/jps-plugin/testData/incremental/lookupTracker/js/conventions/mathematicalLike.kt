package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = /*p:foo.bar(A)*/a

    /*p:foo.bar(A)*/d/*c:foo.bar.A(inc)*/++
    /*c:foo.bar.A(inc) p:foo.bar(A)*/++/*p:foo.bar(A)*/d
    /*p:foo.bar(A)*/d/*c:foo.bar.A(dec) p:foo.bar(dec)*/--
    /*c:foo.bar.A(dec) p:foo.bar(dec) p:foo.bar(A)*/--/*p:foo.bar(A)*/d

    /*p:foo.bar(A)*/a /*c:foo.bar.A(plus)*/+ /*p:kotlin(Int)*/b
    /*p:foo.bar(A)*/a /*c:foo.bar.A(minus) p:foo.bar(minus)*/- /*p:kotlin(Int)*/b
    /*c:foo.bar.A(not) p:foo.bar(not)*/!/*p:foo.bar(A)*/a

    // for val
    /*p:foo.bar(A)*/a /*c:foo.bar.A(timesAssign)*/*= /*p:kotlin(Int)*/b
    /*p:foo.bar(A)*/a /*c:foo.bar.A(divAssign) p:foo.bar(divAssign)*//= /*p:kotlin(Int)*/b

    // for var
    /*p:foo.bar(A)*/d /*c:foo.bar.A(plusAssign) p:foo.bar(plusAssign) c:foo.bar.A(plus)*/+= /*p:kotlin(Int)*/b
    /*p:foo.bar(A)*/d /*c:foo.bar.A(minusAssign) p:foo.bar(minusAssign) c:foo.bar.A(minus) p:foo.bar(minus)*/-= /*p:kotlin(Int)*/b
    /*p:foo.bar(A)*/d /*c:foo.bar.A(timesAssign) c:foo.bar.A(times) p:foo.bar(times)*/*= /*p:kotlin(Int)*/b
    /*p:foo.bar(A)*/d /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) c:foo.bar.A(div) p:foo.bar(div)*//= /*p:kotlin(Int)*/b
}
