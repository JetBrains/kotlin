define("lib", [], function() {
    function A(ss) {
        this.s = ss || "A"
    }
    A.prototype.foo = function (y) {
        return y || "K";
    };
    A.prototype.bar = function (y) {
        return y || "O";
    };

    return A;
});