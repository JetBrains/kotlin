package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = a

    d/*c:foo.bar.A(inc) p:foo.bar(inc) p:java.lang(inc) p:kotlin(inc) p:kotlin.annotation(inc) p:kotlin.jvm(inc) p:kotlin.io(inc) c:foo.bar.A(getInc) c:foo.bar.A(getINC)*/++
    /*c:foo.bar.A(inc) p:foo.bar(inc) p:java.lang(inc) p:kotlin(inc) p:kotlin.annotation(inc) p:kotlin.jvm(inc) p:kotlin.io(inc) c:foo.bar.A(getInc) c:foo.bar.A(getINC)*/++d
    d/*c:foo.bar.A(dec) p:foo.bar(dec) p:java.lang(dec) p:kotlin(dec) p:kotlin.annotation(dec) p:kotlin.jvm(dec) p:kotlin.io(dec) c:foo.bar.A(getDec) c:foo.bar.A(getDEC)*/--
    /*c:foo.bar.A(dec) p:foo.bar(dec) p:java.lang(dec) p:kotlin(dec) p:kotlin.annotation(dec) p:kotlin.jvm(dec) p:kotlin.io(dec) c:foo.bar.A(getDec) c:foo.bar.A(getDEC)*/--d

    a /*c:foo.bar.A(plus) p:foo.bar(plus) p:java.lang(plus) p:kotlin(plus) p:kotlin.annotation(plus) p:kotlin.jvm(plus) p:kotlin.io(plus) c:foo.bar.A(getPlus) c:foo.bar.A(getPLUS)*/+ b
    a /*c:foo.bar.A(minus) p:foo.bar(minus) p:java.lang(minus) p:kotlin(minus) p:kotlin.annotation(minus) p:kotlin.jvm(minus) p:kotlin.io(minus) c:foo.bar.A(getMinus) c:foo.bar.A(getMINUS)*/- b
    /*c:foo.bar.A(not) p:foo.bar(not) p:java.lang(not) p:kotlin(not) p:kotlin.annotation(not) p:kotlin.jvm(not) p:kotlin.io(not) c:foo.bar.A(getNot) c:foo.bar.A(getNOT)*/!a

    // for val
    a /*c:foo.bar.A(timesAssign) p:foo.bar(timesAssign) p:java.lang(timesAssign) p:kotlin(timesAssign) p:kotlin.annotation(timesAssign) p:kotlin.jvm(timesAssign) p:kotlin.io(timesAssign) c:foo.bar.A(getTimesAssign) c:foo.bar.A(getTIMESAssign)*/*= b
    a /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) p:java.lang(divAssign) p:kotlin(divAssign) p:kotlin.annotation(divAssign) p:kotlin.jvm(divAssign) p:kotlin.io(divAssign) c:foo.bar.A(getDivAssign) c:foo.bar.A(getDIVAssign)*//= b

    // for var
    d /*c:foo.bar.A(plusAssign) p:foo.bar(plusAssign) p:java.lang(plusAssign) p:kotlin(plusAssign) p:kotlin.annotation(plusAssign) p:kotlin.jvm(plusAssign) p:kotlin.io(plusAssign) c:foo.bar.A(getPlusAssign) c:foo.bar.A(getPLUSAssign) c:foo.bar.A(plus) p:foo.bar(plus) p:java.lang(plus) p:kotlin(plus) p:kotlin.annotation(plus) p:kotlin.jvm(plus) p:kotlin.io(plus) c:foo.bar.A(getPlus) c:foo.bar.A(getPLUS)*/+= b
    d /*c:foo.bar.A(minusAssign) p:foo.bar(minusAssign) p:java.lang(minusAssign) p:kotlin(minusAssign) p:kotlin.annotation(minusAssign) p:kotlin.jvm(minusAssign) p:kotlin.io(minusAssign) c:foo.bar.A(getMinusAssign) c:foo.bar.A(getMINUSAssign) c:foo.bar.A(minus) p:foo.bar(minus) p:java.lang(minus) p:kotlin(minus) p:kotlin.annotation(minus) p:kotlin.jvm(minus) p:kotlin.io(minus) c:foo.bar.A(getMinus) c:foo.bar.A(getMINUS)*/-= b
    d /*c:foo.bar.A(timesAssign) p:foo.bar(timesAssign) p:java.lang(timesAssign) p:kotlin(timesAssign) p:kotlin.annotation(timesAssign) p:kotlin.jvm(timesAssign) p:kotlin.io(timesAssign) c:foo.bar.A(getTimesAssign) c:foo.bar.A(getTIMESAssign) c:foo.bar.A(times) p:foo.bar(times) p:java.lang(times) p:kotlin(times) p:kotlin.annotation(times) p:kotlin.jvm(times) p:kotlin.io(times) c:foo.bar.A(getTimes) c:foo.bar.A(getTIMES)*/*= b
    d /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) p:java.lang(divAssign) p:kotlin(divAssign) p:kotlin.annotation(divAssign) p:kotlin.jvm(divAssign) p:kotlin.io(divAssign) c:foo.bar.A(getDivAssign) c:foo.bar.A(getDIVAssign) c:foo.bar.A(div) p:foo.bar(div) p:java.lang(div) p:kotlin(div) p:kotlin.annotation(div) p:kotlin.jvm(div) p:kotlin.io(div) c:foo.bar.A(getDiv) c:foo.bar.A(getDIV)*//= b
}
