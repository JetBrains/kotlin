define("lib", [], function () {
    function Foo(ss) {
        this.s = ss || "A"
    }
    Foo.prototype.foo = function (y) {
        return y || "K";
    };
    Foo.prototype.bar = function (y) {
        return y || "O";
    };

    return { Foo };
})
