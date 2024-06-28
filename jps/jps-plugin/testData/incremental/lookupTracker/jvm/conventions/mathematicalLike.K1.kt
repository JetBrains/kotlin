package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = /*p:foo.bar(A)*/a

    /*p:foo.bar(A)*/d/*p:foo.bar.A(inc)*/++
    /*p:foo.bar(A) p:foo.bar.A(inc)*/++/*p:foo.bar(A)*/d
    /*p:foo.bar(A)*/d/*p:foo.bar(dec) p:foo.bar.A(dec) p:foo.bar.A(getDEC) p:foo.bar.A(getDec)*/--
    /*p:foo.bar(A) p:foo.bar(dec) p:foo.bar.A(dec) p:foo.bar.A(getDEC) p:foo.bar.A(getDec)*/--/*p:foo.bar(A)*/d

    /*p:foo.bar(A)*/a /*p:foo.bar.A(plus)*/+ b
    /*p:foo.bar(A)*/a /*p:foo.bar(minus) p:foo.bar.A(getMINUS) p:foo.bar.A(getMinus) p:foo.bar.A(minus)*/- b
    /*p:foo.bar(not) p:foo.bar.A(getNOT) p:foo.bar.A(getNot) p:foo.bar.A(not)*/!/*p:foo.bar(A)*/a

    // for val
    /*p:foo.bar(A)*/a /*p:foo.bar.A(timesAssign)*/*= b
    /*p:foo.bar(A)*/a /*p:foo.bar(divAssign) p:foo.bar.A(divAssign) p:foo.bar.A(getDIVAssign) p:foo.bar.A(getDivAssign)*//= b

    // for var
    /*p:foo.bar(A)*/d /*p:foo.bar(plusAssign) p:foo.bar.A(getPLUSAssign) p:foo.bar.A(getPlusAssign) p:foo.bar.A(plus) p:foo.bar.A(plusAssign) p:java.lang(plusAssign) p:kotlin(plusAssign) p:kotlin.annotation(plusAssign) p:kotlin.collections(plusAssign) p:kotlin.comparisons(plusAssign) p:kotlin.io(plusAssign) p:kotlin.jvm(plusAssign) p:kotlin.ranges(plusAssign) p:kotlin.sequences(plusAssign) p:kotlin.text(plusAssign)*/+= b
    /*p:foo.bar(A)*/d /*p:foo.bar(minus) p:foo.bar(minusAssign) p:foo.bar.A(getMINUS) p:foo.bar.A(getMINUSAssign) p:foo.bar.A(getMinus) p:foo.bar.A(getMinusAssign) p:foo.bar.A(minus) p:foo.bar.A(minusAssign) p:java.lang(minusAssign) p:kotlin(minusAssign) p:kotlin.annotation(minusAssign) p:kotlin.collections(minusAssign) p:kotlin.comparisons(minusAssign) p:kotlin.io(minusAssign) p:kotlin.jvm(minusAssign) p:kotlin.ranges(minusAssign) p:kotlin.sequences(minusAssign) p:kotlin.text(minusAssign)*/-= b
    /*p:foo.bar(A)*/d /*p:foo.bar(times) p:foo.bar.A(getTIMES) p:foo.bar.A(getTimes) p:foo.bar.A(times) p:foo.bar.A(timesAssign) p:java.lang(times) p:kotlin(times) p:kotlin.annotation(times) p:kotlin.collections(times) p:kotlin.comparisons(times) p:kotlin.io(times) p:kotlin.jvm(times) p:kotlin.ranges(times) p:kotlin.sequences(times) p:kotlin.text(times)*/*= b
    /*p:foo.bar(A)*/d /*p:foo.bar(div) p:foo.bar(divAssign) p:foo.bar.A(div) p:foo.bar.A(divAssign) p:foo.bar.A(getDIV) p:foo.bar.A(getDIVAssign) p:foo.bar.A(getDiv) p:foo.bar.A(getDivAssign) p:java.lang(div) p:kotlin(div) p:kotlin.annotation(div) p:kotlin.collections(div) p:kotlin.comparisons(div) p:kotlin.io(div) p:kotlin.jvm(div) p:kotlin.ranges(div) p:kotlin.sequences(div) p:kotlin.text(div)*//= b
}
