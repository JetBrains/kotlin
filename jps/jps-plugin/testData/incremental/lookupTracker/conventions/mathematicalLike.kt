package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = a

    d/*c:foo.bar.A(inc)*/++
    /*c:foo.bar.A(inc)*/++d
    d/*c:foo.bar.A(dec) c:foo.bar.A(getDec) c:foo.bar.A(getDEC) p:foo.bar(dec)*/--
    /*c:foo.bar.A(dec) c:foo.bar.A(getDec) c:foo.bar.A(getDEC) p:foo.bar(dec)*/--d

    a /*c:foo.bar.A(plus)*/+ b
    a /*c:foo.bar.A(minus) c:foo.bar.A(getMinus) c:foo.bar.A(getMINUS) p:foo.bar(minus)*/- b
    /*c:foo.bar.A(not) c:foo.bar.A(getNot) c:foo.bar.A(getNOT) p:foo.bar(not)*/!a

    // for val
    a /*c:foo.bar.A(timesAssign)*/*= b
    a /*c:foo.bar.A(divAssign) c:foo.bar.A(getDivAssign) c:foo.bar.A(getDIVAssign) p:foo.bar(divAssign)*//= b

    // for var
    d /*c:foo.bar.A(plusAssign) c:foo.bar.A(getPlusAssign) c:foo.bar.A(getPLUSAssign) p:foo.bar(plusAssign) p:java.lang(plusAssign) p:kotlin(plusAssign) p:kotlin.annotation(plusAssign) p:kotlin.jvm(plusAssign) p:kotlin.collections(plusAssign) p:kotlin.ranges(plusAssign) p:kotlin.sequences(plusAssign) p:kotlin.text(plusAssign) p:kotlin.io(plusAssign) c:foo.bar.A(plus)*/+= b
    d /*c:foo.bar.A(minusAssign) c:foo.bar.A(getMinusAssign) c:foo.bar.A(getMINUSAssign) p:foo.bar(minusAssign) p:java.lang(minusAssign) p:kotlin(minusAssign) p:kotlin.annotation(minusAssign) p:kotlin.jvm(minusAssign) p:kotlin.collections(minusAssign) p:kotlin.ranges(minusAssign) p:kotlin.sequences(minusAssign) p:kotlin.text(minusAssign) p:kotlin.io(minusAssign) c:foo.bar.A(minus) c:foo.bar.A(getMinus) c:foo.bar.A(getMINUS) p:foo.bar(minus)*/-= b
    d /*c:foo.bar.A(timesAssign) c:foo.bar.A(times) c:foo.bar.A(getTimes) c:foo.bar.A(getTIMES) p:foo.bar(times) p:java.lang(times) p:kotlin(times) p:kotlin.annotation(times) p:kotlin.jvm(times) p:kotlin.collections(times) p:kotlin.ranges(times) p:kotlin.sequences(times) p:kotlin.text(times) p:kotlin.io(times)*/*= b
    d /*c:foo.bar.A(divAssign) c:foo.bar.A(getDivAssign) c:foo.bar.A(getDIVAssign) p:foo.bar(divAssign) c:foo.bar.A(div) c:foo.bar.A(getDiv) c:foo.bar.A(getDIV) p:foo.bar(div) p:java.lang(div) p:kotlin(div) p:kotlin.annotation(div) p:kotlin.jvm(div) p:kotlin.collections(div) p:kotlin.ranges(div) p:kotlin.sequences(div) p:kotlin.text(div) p:kotlin.io(div)*//= b
}
