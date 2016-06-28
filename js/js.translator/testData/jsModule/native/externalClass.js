define("lib", [], function() {
    function A(x) {
        this.x = x;
    }
    A.prototype.foo = function (y) {
        return this.x + y;
    };

    return A;
});