package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Int) {
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
    /*p:foo.bar(A)*/d /*c:foo.bar.A(plusAssign) p:foo.bar(plusAssign) p:kotlin(plusAssign) p:kotlin.annotation(plusAssign) p:kotlin.collections(plusAssign) p:kotlin.ranges(plusAssign) p:kotlin.sequences(plusAssign) p:kotlin.text(plusAssign) p:kotlin.io(plusAssign) p:kotlin.comparisons(plusAssign) p:kotlin.js(plusAssign) c:foo.bar.A(plus)*/+= /*p:kotlin(Int)*/b
    /*p:foo.bar(A)*/d /*c:foo.bar.A(minusAssign) p:foo.bar(minusAssign) p:kotlin(minusAssign) p:kotlin.annotation(minusAssign) p:kotlin.collections(minusAssign) p:kotlin.ranges(minusAssign) p:kotlin.sequences(minusAssign) p:kotlin.text(minusAssign) p:kotlin.io(minusAssign) p:kotlin.comparisons(minusAssign) p:kotlin.js(minusAssign) c:foo.bar.A(minus) p:foo.bar(minus)*/-= /*p:kotlin(Int)*/b
    /*p:foo.bar(A)*/d /*c:foo.bar.A(timesAssign) c:foo.bar.A(times) p:foo.bar(times) p:kotlin(times) p:kotlin.annotation(times) p:kotlin.collections(times) p:kotlin.ranges(times) p:kotlin.sequences(times) p:kotlin.text(times) p:kotlin.io(times) p:kotlin.comparisons(times) p:kotlin.js(times)*/*= /*p:kotlin(Int)*/b
    /*p:foo.bar(A)*/d /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) c:foo.bar.A(div) p:foo.bar(div) p:kotlin(div) p:kotlin.annotation(div) p:kotlin.collections(div) p:kotlin.ranges(div) p:kotlin.sequences(div) p:kotlin.text(div) p:kotlin.io(div) p:kotlin.comparisons(div) p:kotlin.js(div)*//= /*p:kotlin(Int)*/b
}
