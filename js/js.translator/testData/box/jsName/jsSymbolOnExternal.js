function Foo() {
}
Foo.prototype[Symbol.toStringTag] = function () {
    return "K"
}
