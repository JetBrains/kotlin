define("a", [], function() {

    function A() {}

    A.prototype.foo = function () {
        return "O";
    };

    return {
        A: A,
        bar: function() { return 1; },
        prop: 10
    };
});

define("b", [], function() {

    function A() {}

    A.prototype.foo = function () {
        return "K";
    };

    return {
        A: A,
        bar: function() { return 2; },
        prop: 20
    };
});