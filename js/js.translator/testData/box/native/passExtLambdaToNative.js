function A(v) {
    this.v = v;
}

function bar(a, extLambda) {
    return extLambda(a, 4, "boo")
}
