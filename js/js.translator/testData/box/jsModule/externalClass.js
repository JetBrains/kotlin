define("lib", [], function() {
    function A(x) {
        this.x = x;
    }
    A.prototype.foo = function (y) {
        return this.x + y;
    };

    A.prototype.bar = function() {
        return "(" + Array.prototype.join.call(arguments, "") + ")";
    };

    return A;
});