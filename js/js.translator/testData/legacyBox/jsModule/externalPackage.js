define("lib", [], function() {
    function A(x) {
        this.x = x;
    }
    A.prototype.foo = function (y) {
        return this.x + y;
    };

    function Nested() {
        this.y = 55;
    }
    A.Nested = Nested;

    B = {
        x: 123,
        foo: function(y) {
            return this.x + y;
        }
    };

    function foo(y) {
        return 323 + y;
    }
    
    var bar = 423;

    return {
        A: A,
        B: B,
        foo: foo,
        bar: bar,
        mbar: -1
    };
});