function A(v) {
    this.v = v;
}

function nativeBox(b) {
    return b.bar_0(new A("foo"), function(i, s) { return "" + this.v + s + i })
}
