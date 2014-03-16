function A(v) {
    this.v = v;
}

function nativeBox(b) {
    return b.bar(new A("foo"), function(i, s) { return "" + this.v + s + i })
}
