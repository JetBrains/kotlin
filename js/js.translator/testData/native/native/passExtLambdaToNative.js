function A(v) {
    this.v = v;
}

function bar(a, extLambda) {
    return extLambda.call(a, 4, "boo")
}
