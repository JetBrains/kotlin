package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = a

    d/*c:foo.bar.A(inc) p:foo.bar(inc) c:foo.bar.A(getInc) c:foo.bar.A(getINC)*/++
    /*c:foo.bar.A(inc) p:foo.bar(inc) c:foo.bar.A(getInc) c:foo.bar.A(getINC)*/++d
    d/*c:foo.bar.A(dec) p:foo.bar(dec) c:foo.bar.A(getDec) c:foo.bar.A(getDEC)*/--
    /*c:foo.bar.A(dec) p:foo.bar(dec) c:foo.bar.A(getDec) c:foo.bar.A(getDEC)*/--d

    a /*c:foo.bar.A(plus) p:foo.bar(plus) c:foo.bar.A(getPlus) c:foo.bar.A(getPLUS)*/+ b
    a /*c:foo.bar.A(minus) p:foo.bar(minus) c:foo.bar.A(getMinus) c:foo.bar.A(getMINUS)*/- b
    /*c:foo.bar.A(not) p:foo.bar(not) c:foo.bar.A(getNot) c:foo.bar.A(getNOT)*/!a

    // for val
    a /*c:foo.bar.A(timesAssign) p:foo.bar(timesAssign) c:foo.bar.A(getTimesAssign) c:foo.bar.A(getTIMESAssign)*/*= b
    a /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) c:foo.bar.A(getDivAssign) c:foo.bar.A(getDIVAssign)*//= b

    // for var
    d /*c:foo.bar.A(plusAssign) p:foo.bar(plusAssign) c:foo.bar.A(getPlusAssign) c:foo.bar.A(getPLUSAssign) c:foo.bar.A(plus) p:foo.bar(plus) c:foo.bar.A(getPlus) c:foo.bar.A(getPLUS)*/+= b
    d /*c:foo.bar.A(minusAssign) p:foo.bar(minusAssign) c:foo.bar.A(getMinusAssign) c:foo.bar.A(getMINUSAssign) c:foo.bar.A(minus) p:foo.bar(minus) c:foo.bar.A(getMinus) c:foo.bar.A(getMINUS)*/-= b
    d /*c:foo.bar.A(timesAssign) p:foo.bar(timesAssign) c:foo.bar.A(getTimesAssign) c:foo.bar.A(getTIMESAssign) c:foo.bar.A(times) p:foo.bar(times) c:foo.bar.A(getTimes) c:foo.bar.A(getTIMES)*/*= b
    d /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) c:foo.bar.A(getDivAssign) c:foo.bar.A(getDIVAssign) c:foo.bar.A(div) p:foo.bar(div) c:foo.bar.A(getDiv) c:foo.bar.A(getDIV)*//= b
}
